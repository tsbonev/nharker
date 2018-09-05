package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.*
import com.tsbonev.nharker.core.exceptions.*
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

        if(article.entries[entryId] != null) throw EntryAlreadyInArticleException()

        val possibleEntry = entryService.getById(entryId)

        if(!possibleEntry.isPresent) throw EntryNotFoundException()

        val retrievedEntry = possibleEntry.get()

        val updatedArticle = article
                .copy(entries = article.entries.plus(
                        retrievedEntry.id to article.entries.count()
                ))

        val updatedEntry = entryService.changeArticle(retrievedEntry.id, articleId)

        coll.update(updatedArticle)

        return updatedEntry
    }

    override fun removeEntry(articleId: String, entryId: String): Entry {
        val article = findByIdOrThrow(articleId)

        val entryToDelete = entryService.getById(entryId)

        if(!entryToDelete.isPresent) throw EntryNotFoundException()

        val articleEntries = article.entries.toMutableMap()
        val deletedSpace = articleEntries[entryId] ?: throw EntryNotInArticleException()
        articleEntries.remove(entryId)

        val updatedArticle = article.copy(entries = reorderEntriesAfterDeletion(articleEntries, deletedSpace))
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

    private fun reorderEntriesAfterDeletion(entries: MutableMap<String, Int>,
                                            deletedSpace: Int): MutableMap<String, Int>{
        entries.forEach {
            if(it.value > deletedSpace) entries[it.key] = it.value - 1
        }
        return entries
    }

    private fun findByIdOrThrow(articleId: String): Article{
        return coll.find(Article::id eq articleId).firstOrNull() ?: throw ArticleNotFoundException()
    }
}