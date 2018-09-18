package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.*
import com.tsbonev.nharker.core.exceptions.*
import com.tsbonev.nharker.core.helpers.ElementNotInMapException
import com.tsbonev.nharker.core.helpers.append
import com.tsbonev.nharker.core.helpers.subtract
import com.tsbonev.nharker.core.helpers.switch
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
class NitriteCatalogues(private val nitriteDb: Nitrite,
                        private val collectionName: String = "Catalogues",
                        private val clock: Clock = Clock.systemUTC()) : Catalogues {
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
                LocalDateTime.now(clock),
                parentCatalogue = catalogueRequest.parentCatalogue
        )

        coll.insert(catalogue)
        return catalogue
    }

    override fun save(catalogue: Catalogue): Catalogue {
        coll.update(catalogue, true)
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

    override fun changeParentCatalogue(catalogueId: String, parentCatalogue: Catalogue): Catalogue {
        val childCatalogue = findOrThrow(catalogueId)

        if(childCatalogue.parentCatalogue == parentCatalogue.id) throw CatalogueAlreadyAChildException()
        if(childCatalogue.id == parentCatalogue.id) throw SelfContainedCatalogueException()

        val updatedChild = childCatalogue.copy(parentCatalogue = parentCatalogue.id)
        val updatedParent = parentCatalogue.copy(subCatalogues = parentCatalogue.subCatalogues
                .append(catalogueId))

        coll.update(updatedChild)
        coll.update(updatedParent)

        return updatedChild
    }

    override fun delete(catalogueId: String): Catalogue {
        val catalogue = findOrThrow(catalogueId)

        coll.remove(Catalogue::id eq catalogueId)

        return catalogue
    }

    override fun appendSubCatalogue(catalogueId: String, subCatalogue: Catalogue): Catalogue {
        if(subCatalogue.parentCatalogue == catalogueId) throw CatalogueAlreadyAChildException()

        if(subCatalogue.id == catalogueId) throw SelfContainedCatalogueException()

        val parentCatalogue = findOrThrow(catalogueId)

        val updatedChild = subCatalogue.copy(parentCatalogue = catalogueId)
        val updatedParent = parentCatalogue.copy(subCatalogues = parentCatalogue.subCatalogues.append(subCatalogue.id))

        coll.update(updatedChild)
        coll.update(updatedParent)

        return updatedChild
    }

    override fun removeSubCatalogue(catalogueId: String, subCatalogue: Catalogue): Catalogue {
        if(subCatalogue.parentCatalogue != catalogueId) throw CatalogueNotAChildException()

        val parentCatalogue = findOrThrow(catalogueId)

        val updatedChild = subCatalogue.copy(parentCatalogue = "None")

        val updatedParent = parentCatalogue.copy(subCatalogues = parentCatalogue.subCatalogues.subtract(subCatalogue.id))

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

    override fun switchArticles(catalogueId: String, first: Article, second: Article): Catalogue {
        val catalogue = findOrThrow(catalogueId)

        return try {
            val updatedCatalogue = catalogue.copy(articles = catalogue.articles.switch(first.id, second.id))
            coll.update(updatedCatalogue)
            updatedCatalogue
        }catch (ex: ElementNotInMapException){
            throw ArticleNotInCatalogueException()
        }
    }

    override fun switchSubCatalogues(catalogueId: String, first: Catalogue, second: Catalogue): Catalogue {
        val catalogue = findOrThrow(catalogueId)

        return try {
            val updatedCatalogue = catalogue.copy(subCatalogues = catalogue.subCatalogues.switch(first.id, second.id))
            coll.update(updatedCatalogue)
            updatedCatalogue
        }catch (ex: ElementNotInMapException){
            throw CatalogueNotAChildException()
        }
    }

    private fun findOrThrow(catalogueId: String): Catalogue{
        return coll.find(Catalogue::id eq catalogueId).firstOrNull() ?: throw CatalogueNotFoundException()
    }
}