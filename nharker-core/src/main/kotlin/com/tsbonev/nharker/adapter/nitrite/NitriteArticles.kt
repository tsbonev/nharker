package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.*
import com.tsbonev.nharker.core.helpers.*
import org.dizitart.kno2.filters.eq
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
                      private val clock: Clock = Clock.systemUTC()) : Articles {

    /**
     * Retrieve the repository on every request.
     */
    private val coll: ObjectRepository<Article>
        get() = nitriteDb.getRepository(collectionName, Article::class.java)

    @Throws(ArticleTitleTakenException::class)
    override fun create(articleRequest: ArticleRequest): Article {
        val article = Article(
                NitriteId.newId().toString(),
                articleRequest.title.toLinkTitle(),
                articleRequest.title,
                LocalDateTime.now(clock),
                articleRequest.catalogue
        )

        if(coll.find(Article::linkTitle eq article.linkTitle).firstOrNull() != null)
            throw ArticleTitleTakenException()

        coll.insert(article)
        return article
    }

    override fun save(article: Article): Article {
        coll.update(article, true)
        return article
    }

    override fun getById(articleId: String): Optional<Article> {
        val article = coll.find(Article::id eq articleId).firstOrNull() ?: return Optional.empty()
        return Optional.of(article)
    }

    override fun appendEntry(articleId: String, entry: Entry): Entry {
        val article = findByIdOrThrow(articleId)

        if(article.entries.containsKey(entry.id)) throw EntryAlreadyInArticleException()

        val updatedArticle = article
                .copy(entries = article.entries.append(entry.id))

        coll.update(updatedArticle)

        return entry.copy(articleId = articleId)
    }

    override fun removeEntry(articleId: String, entry: Entry): Entry {
        val article = findByIdOrThrow(articleId)

        if(!article.entries.contains(entry.id)) throw EntryNotInArticleException()

        val updatedArticle = article.copy(entries = article.entries.subtract(entry.id))
        coll.update(updatedArticle)

        return entry.copy(articleId = ReferenceId.Deleted.value)
    }

    override fun setCatalogue(articleId: String, catalogue: Catalogue): Catalogue {
        val article = findByIdOrThrow(articleId)

        if(article.catalogueId == catalogue.id) throw ArticleAlreadyInCatalogueException()

        val updatedArticle = article.copy(catalogueId = catalogue.id)

        coll.update(updatedArticle)

        return catalogue.copy(articles = catalogue.articles.append(articleId))
    }

    override fun switchEntries(articleId: String, first: Entry, second: Entry): Article {
        val article = findByIdOrThrow(articleId)

        return try {
            val updatedArticle = article.copy(entries = article.entries.switch(first.id, second.id))
            coll.update(updatedArticle)
            updatedArticle
        }catch (ex: ElementNotInMapException){
            throw EntryNotInArticleException()
        }
    }

    private fun findByIdOrThrow(articleId: String): Article{
        return coll.find(Article::id eq articleId).firstOrNull() ?: throw ArticleNotFoundException()
    }
}