package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.*
import com.tsbonev.nharker.core.exceptions.*
import com.tsbonev.nharker.core.helpers.append
import com.tsbonev.nharker.core.helpers.subtract
import org.dizitart.kno2.filters.eq
import org.dizitart.no2.Nitrite
import org.dizitart.no2.NitriteId
import org.dizitart.no2.objects.ObjectRepository
import java.time.LocalDateTime
import java.util.Optional

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteArticles(private val nitriteDb: Nitrite,
                      private val entryService: EntryService,
                      private val catalogueService: CatalogueService,
                      private val collectionName: String = "Articles",
                      private val getInstant: () -> LocalDateTime = { LocalDateTime.now()}) : Articles {

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
                getInstant(),
                articleRequest.catalogue
        )

        if(coll.find(Article::linkTitle eq article.linkTitle).firstOrNull() != null)
            throw ArticleTitleTakenException()

        coll.insert(article)
        return article
    }

    override fun getById(articleId: String): Optional<Article> {
        val article = coll.find(Article::id eq articleId).firstOrNull() ?: return Optional.empty()
        return Optional.of(article)
    }

    override fun appendEntry(articleId: String, entryId: String): Entry {
        val article = findByIdOrThrow(articleId)

        if(article.entries.containsKey(entryId)) throw EntryAlreadyInArticleException()

        val possibleEntry = entryService.getById(entryId)

        if(!possibleEntry.isPresent) throw EntryNotFoundException()

        val retrievedEntry = possibleEntry.get()

        val updatedArticle = article
                .copy(entries = article.entries.append(entryId))

        val updatedEntry = entryService.changeArticle(retrievedEntry.id, articleId)

        coll.update(updatedArticle)

        return updatedEntry
    }

    override fun removeEntry(articleId: String, entryId: String): Entry {
        val article = findByIdOrThrow(articleId)

        val entryToDelete = entryService.getById(entryId)

        if(!entryToDelete.isPresent) throw EntryNotFoundException()

        if(!article.entries.contains(entryId)) throw EntryNotInArticleException()

        val updatedArticle = article.copy(entries = article.entries.subtract(entryId))
        coll.update(updatedArticle)

        return entryService.changeArticle(entryId, "deleted")
    }

    override fun setCatalogue(articleId: String, catalogueId: String): Article {
        val article = findByIdOrThrow(articleId)

        if(article.catalogueId == catalogueId) throw ArticleAlreadyInCatalogueException()

        val possibleCatalogue = catalogueService.getById(catalogueId)
        if(!possibleCatalogue.isPresent) throw CatalogueNotFoundException()

        val updatedArticle = article.copy(catalogueId = possibleCatalogue.get().id)

        coll.update(updatedArticle)

        return updatedArticle
    }

    private fun findByIdOrThrow(articleId: String): Article{
        return coll.find(Article::id eq articleId).firstOrNull() ?: throw ArticleNotFoundException()
    }
}