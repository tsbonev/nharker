package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleFullTitle
import com.tsbonev.nharker.core.ArticleLinkTitle
import com.tsbonev.nharker.core.ArticleNotFoundException
import com.tsbonev.nharker.core.ArticleRequest
import com.tsbonev.nharker.core.ArticleTitleTakenException
import com.tsbonev.nharker.core.Articles
import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.core.ElementNotInMapException
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryAlreadyInArticleException
import com.tsbonev.nharker.core.EntryLinker
import com.tsbonev.nharker.core.EntryNotInArticleException
import com.tsbonev.nharker.core.Paginator
import com.tsbonev.nharker.core.SortBy
import com.tsbonev.nharker.core.toLinkTitle
import org.dizitart.kno2.filters.elemMatch
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.filters.text
import org.dizitart.no2.Nitrite
import org.dizitart.no2.NitriteId
import org.dizitart.no2.objects.ObjectRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.Optional

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteArticles(private val nitriteDb: Nitrite,
                      private val collectionName: String = "Articles",
                      private val entryLinker: EntryLinker,
                      private val clock: Clock = Clock.systemUTC())
    : Articles, Paginator<Article> {

    private val coll: ObjectRepository<Article>
        get() = nitriteDb.getRepository(collectionName, Article::class.java)

    private val paginator: Paginator<Article> by lazy {
        NitritePaginator(coll)
    }

    override fun create(articleRequest: ArticleRequest): Article {
        val article = Article(
                NitriteId.newId().toString(),
                articleRequest.fullTitle.toLinkTitle(),
                articleRequest.fullTitle,
                LocalDateTime.now(clock)
        )

        if (coll.find(Article::linkTitle eq article.linkTitle).firstOrNull() != null)
            throw ArticleTitleTakenException(articleRequest.fullTitle)

        coll.insert(article)
        return article
    }

    override fun save(article: Article): Article {
        coll.update(article, true)
        return article
    }

    override fun getAll(order: SortBy): List<Article> {
        return paginator.getAll(order)
    }

    override fun getAll(order: SortBy, page: Int, pageSize: Int): List<Article> {
        return paginator.getAll(order, page, pageSize)
    }

    override fun getById(articleId: String): Optional<Article> {
        val article = coll.find(Article::id eq articleId).firstOrNull()
                ?: return Optional.empty()

        return Optional.of(article)
    }

    override fun getByCatalogue(catalogue: Catalogue): List<Article> {
        return coll.find(Article::catalogues elemMatch (Article::catalogues eq catalogue.id)).toList()
    }

    override fun getByLinkTitle(linkTitle: String): Optional<Article> {
        val article = coll.find(Article::linkTitle eq linkTitle).firstOrNull()
                ?: return Optional.empty()

        return Optional.of(article)
    }

    override fun delete(articleId: String): Article {
        val article = findByIdOrThrow(articleId)

        coll.remove(article)
        return article
    }

    override fun addCatalogue(articleId: String, catalogue: Catalogue): Article {
        val article = findByIdOrThrow(articleId)

        val updatedArticle = article.copy(catalogues = article.catalogues.plus(catalogue.id))

        coll.update(updatedArticle)

        return updatedArticle
    }

    override fun removeCatalogue(articleId: String, catalogue: Catalogue): Article {
        val article = findByIdOrThrow(articleId)

        val updatedArticle = article.copy(catalogues = article.catalogues.minus(catalogue.id))

        coll.update(updatedArticle)

        return updatedArticle
    }

    override fun appendEntry(articleId: String, entry: Entry): Article {
        val article = findByIdOrThrow(articleId)

        if (article.entries.raw().containsKey(entry.id))
            throw EntryAlreadyInArticleException(entry.id, articleId)

        handleArticleLinks(article, entry, true)

        article.entries.append(entry.id)

        coll.update(article)
        return article
    }

    override fun removeEntry(articleId: String, entry: Entry): Article {
        val article = findByIdOrThrow(articleId)

        if (!article.entries.contains(entry.id))
            throw EntryNotInArticleException(entry.id, articleId)

        handleArticleLinks(article, entry, false)

        article.entries.subtract(entry.id)

        coll.update(article)
        return article
    }

    override fun switchEntries(articleId: String, first: Entry, second: Entry): Article {
        val article = findByIdOrThrow(articleId)

        return try {
            article.entries.switch(first.id, second.id)

            coll.update(article)
            article
        } catch (ex: ElementNotInMapException) {
            throw EntryNotInArticleException(ex.reference, articleId)
        }
    }

    override fun attachProperty(articleId: String, propertyName: String, property: Entry): Article {
        val article = findByIdOrThrow(articleId)

        article.properties.attachProperty(propertyName, property.id)

        coll.update(article)
        return article
    }

    override fun detachProperty(articleId: String, propertyName: String): Article {
        val article = findByIdOrThrow(articleId)

        article.properties.detachProperty(propertyName)

        coll.update(article)
        return article
    }

    override fun searchByFullTitle(searchString: String): List<Article> {
        return coll.find(Article::fullTitle text searchString).toList()
    }

    override fun getArticleTitles(linkTitleList: Set<String>): List<String> {
        val fullTitleList = mutableListOf<String>()

        linkTitleList.forEach {
            val article = coll.find(Article::linkTitle eq it)
                    .project(ArticleFullTitle::class.java).firstOrNull() ?: return@forEach
            fullTitleList.add(article.fullTitle)
        }

        return fullTitleList
    }

    /**
     * Finds an article by id or throws an exception.
     */
    private fun findByIdOrThrow(articleId: String): Article {
        return coll.find(Article::id eq articleId).firstOrNull()
                ?: throw ArticleNotFoundException(articleId)
    }

    /**
     * Returns a list of all articles' link titles.
     *
     * @return List of article link titles.
     */
    private fun getArticleLinkTitles(): List<String> {
        val projectedArticleTitles = coll
                .find()
                .project(ArticleLinkTitle::class.java)
                .toList()

        val articleTitles = mutableListOf<String>()

        projectedArticleTitles.forEach { articleTitles.add(it.linkTitle) }

        return articleTitles
    }

    /**
     * Removes or adds a link to the article's links depending on the passed
     * boolean.
     *
     * @param article The article to modify.
     * @param entry The entry whose links are up for modification.
     * @param adding Whether or not the links should be added or removed.
     */
    private fun handleArticleLinks(article: Article, entry: Entry, adding: Boolean) {
        val entryLinks = entryLinker.findArticleLinks(
                entry,
                getArticleLinkTitles()
        )

        if (adding) {
            entryLinks.forEach {
                article.links.addLink(it)
            }
        } else {
            entryLinks.forEach {
                article.links.removeLink(it)
            }
        }
    }
}