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
class NitriteCatalogues(private val nitriteDb: Nitrite,
                        private val articleService: ArticleService,
                        private val collectionName: String = "Catalogues",
                        private val getInstant: () -> LocalDateTime = { LocalDateTime.now()}) : Catalogues {
    /**
     * Retrieve the repository on every request.
     */
    private val coll: ObjectRepository<Catalogue>
        get() = nitriteDb.getRepository(collectionName, Catalogue::class.java)

    override fun create(catalogueRequest: CatalogueRequest): Catalogue {
        if(coll.find(Catalogue::title eq catalogueRequest.title).firstOrNull() != null)
            throw CatalogueTitleTakenException()

        val catalogue = Catalogue(
                NitriteId.newId().toString(),
                catalogueRequest.title,
                getInstant(),
                parentCatalogue = catalogueRequest.parentCatalogue
        )

        coll.insert(catalogue)
        return catalogue
    }

    override fun getById(catalogueId: String): Optional<Catalogue> {
        val catalogue = coll.find(Catalogue::id eq catalogueId).firstOrNull() ?: return Optional.empty()
        return Optional.of(catalogue)
    }

    override fun changeTitle(catalogueId: String, newTitle: String): Catalogue {
        val catalogue = findOrThrow(catalogueId)

        if(coll.find(Catalogue::title eq newTitle).firstOrNull() != null) throw CatalogueTitleTakenException()

        val updatedCatalogue = catalogue.copy(title = newTitle)

        coll.update(updatedCatalogue)
        return updatedCatalogue
    }

    override fun changeParentCatalogue(catalogueId: String, parentCatalogueId: String): Pair<Catalogue, Catalogue> {
        val childCatalogue = findOrThrow(catalogueId)

        if(childCatalogue.parentCatalogue == parentCatalogueId) throw CatalogueIsAlreadyAChildException()

        val parentCatalogue = findOrThrow(parentCatalogueId)

        val updatedChild = childCatalogue.copy(parentCatalogue = parentCatalogueId)
        val updatedParent = parentCatalogue.copy(subCatalogues = parentCatalogue.subCatalogues
                .plus(Pair(catalogueId, parentCatalogue.subCatalogues.count())))

        coll.update(updatedChild)
        coll.update(updatedParent)

        return (updatedParent to updatedChild)
    }

    override fun delete(catalogueId: String): Catalogue {
        val catalogue = findOrThrow(catalogueId)

        coll.remove(Catalogue::id eq catalogueId)

        return catalogue
    }

    override fun appendSubcatalogue(parentCatalogueId: String, subCatalogueId: String): Catalogue {
        val childCatalogue = findOrThrow(subCatalogueId)

        if(childCatalogue.parentCatalogue == parentCatalogueId) throw CatalogueIsAlreadyAChildException()

        val parentCatalogue = findOrThrow(parentCatalogueId)

        val updatedChild = childCatalogue.copy(parentCatalogue = parentCatalogueId)
        val updatedParent = parentCatalogue.copy(subCatalogues = parentCatalogue.subCatalogues.plus(
                subCatalogueId to parentCatalogue.subCatalogues.count()
        ))

        coll.update(updatedChild)
        coll.update(updatedParent)

        return updatedChild
    }

    override fun removeSubCatalogue(parentCatalogueId: String, subCatalogueId: String): Catalogue {
        val childCatalogue = findOrThrow(subCatalogueId)

        if(childCatalogue.parentCatalogue != parentCatalogueId) throw CatalogueIsNotAChildException()

        val parentCatalogue = findOrThrow(parentCatalogueId)

        val updatedChild = childCatalogue.copy(parentCatalogue = "None")

        val deletedSpace = parentCatalogue.subCatalogues[subCatalogueId]!!

        val updatedParentSubcatalogues = parentCatalogue.subCatalogues.toMutableMap()

        updatedParentSubcatalogues.remove(subCatalogueId)

        val updatedParent = parentCatalogue.copy(
                subCatalogues = reorderMapAfterDeletion(updatedParentSubcatalogues, deletedSpace))

        coll.update(updatedChild)
        coll.update(updatedParent)

        return updatedChild
    }

    override fun appendArticle(catalogueId: String, articleId: String): Article {
        val possibleArticle = articleService.getById(articleId)

        if(!possibleArticle.isPresent) throw ArticleNotFoundException()

        val retrievedArticle = possibleArticle.get()
        if(retrievedArticle.catalogueId == catalogueId) throw ArticleAlreadyInCatalogueException()

        val catalogue = findOrThrow(catalogueId)

        val updatedCatalogue = catalogue.copy(articles = catalogue.articles.plus(
                articleId to catalogue.articles.count()
        ))

        articleService.setCatalogue(articleId, catalogueId)
        val updatedArticle = retrievedArticle.copy(catalogueId = catalogueId)

        coll.update(updatedCatalogue)

        return updatedArticle
    }

    override fun removeArticle(catalogueId: String, articleId: String): Article {
        val possibleArticle = articleService.getById(articleId)

        if(!possibleArticle.isPresent) throw ArticleNotFoundException()

        val retrievedArticle = possibleArticle.get()
        if(retrievedArticle.catalogueId != catalogueId) throw ArticleNotInCatalogueException()

        val catalogue = findOrThrow(catalogueId)

        val deletedArticleSpace = catalogue.articles[articleId]!!

        val catalogueMap = catalogue.articles.toMutableMap()
        catalogueMap.remove(articleId)

        val updatedCatalogue = catalogue.copy(articles = reorderMapAfterDeletion(catalogueMap, deletedArticleSpace))

        coll.update(updatedCatalogue)

        articleService.setCatalogue(articleId, "none")

        return retrievedArticle.copy(catalogueId = "none")
    }

    private fun reorderMapAfterDeletion(map: MutableMap<String, Int>,
                                            deletedSpace: Int): MutableMap<String, Int>{
        map.forEach {
            if(it.value > deletedSpace) map[it.key] = it.value - 1
        }
        return map
    }

    private fun findOrThrow(catalogueId: String): Catalogue{
        return coll.find(Catalogue::id eq catalogueId).firstOrNull() ?: throw CatalogueNotFoundException()
    }
}