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
class NitriteCatalogues(private val nitriteDb: Nitrite,
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
                .append(catalogueId))

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
        val updatedParent = parentCatalogue.copy(subCatalogues = parentCatalogue.subCatalogues.append(subCatalogueId))

        coll.update(updatedChild)
        coll.update(updatedParent)

        return updatedChild
    }

    override fun removeSubCatalogue(parentCatalogueId: String, subCatalogueId: String): Catalogue {
        val childCatalogue = findOrThrow(subCatalogueId)

        if(childCatalogue.parentCatalogue != parentCatalogueId) throw CatalogueIsNotAChildException()

        val parentCatalogue = findOrThrow(parentCatalogueId)

        val updatedChild = childCatalogue.copy(parentCatalogue = "None")

        val updatedParent = parentCatalogue.copy(subCatalogues = parentCatalogue.subCatalogues.subtract(subCatalogueId))

        coll.update(updatedChild)
        coll.update(updatedParent)

        return updatedChild
    }

    override fun appendArticle(catalogueId: String, article: Article): Article {
        if(article.catalogueId == catalogueId) throw ArticleAlreadyInCatalogueException()

        val catalogue = findOrThrow(catalogueId)

        val updatedCatalogue = catalogue.copy(articles = catalogue.articles.append(article.id))

        coll.update(updatedCatalogue)

        return article.copy(catalogueId = catalogueId)
    }

    override fun removeArticle(catalogueId: String, article: Article): Article {
        if(article.catalogueId != catalogueId) throw ArticleNotInCatalogueException()

        val catalogue = findOrThrow(catalogueId)

        val updatedCatalogue = catalogue.copy(articles = catalogue.articles.subtract(article.id))

        coll.update(updatedCatalogue)

        return article.copy(catalogueId = "none")
    }

    private fun findOrThrow(catalogueId: String): Catalogue{
        return coll.find(Catalogue::id eq catalogueId).firstOrNull() ?: throw CatalogueNotFoundException()
    }
}