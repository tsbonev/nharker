package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.core.CatalogueAlreadyAChildException
import com.tsbonev.nharker.core.CatalogueCircularInheritanceException
import com.tsbonev.nharker.core.CatalogueNotAChildException
import com.tsbonev.nharker.core.CatalogueNotFoundException
import com.tsbonev.nharker.core.CatalogueRequest
import com.tsbonev.nharker.core.CatalogueTitleTakenException
import com.tsbonev.nharker.core.Catalogues
import com.tsbonev.nharker.core.ElementNotInMapException
import com.tsbonev.nharker.core.Paginator
import com.tsbonev.nharker.core.SelfContainedCatalogueException
import com.tsbonev.nharker.core.SortBy
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
                        private val clock: Clock = Clock.systemUTC())
    : Catalogues, Paginator<Catalogue> {

    private val coll: ObjectRepository<Catalogue>
        get() = nitriteDb.getRepository(collectionName, Catalogue::class.java)

    private val paginator: Paginator<Catalogue> by lazy {
        NitritePaginator(coll)
    }

    override fun create(catalogueRequest: CatalogueRequest): Catalogue {
        if (coll.find(Catalogue::title eq catalogueRequest.title).firstOrNull() != null)
            throw CatalogueTitleTakenException(catalogueRequest.title)

        if (catalogueRequest.parentId != null
                && coll.find(Catalogue::title eq catalogueRequest.parentId).firstOrNull() == null)
            throw CatalogueNotFoundException(catalogueRequest.parentId)

        val catalogue = Catalogue(
                NitriteId.newId().toString(),
                catalogueRequest.title,
                LocalDateTime.now(clock),
                parentId = catalogueRequest.parentId
        )

        coll.insert(catalogue)
        return catalogue
    }

    override fun save(catalogue: Catalogue): Catalogue {
        coll.update(catalogue, true)
        return catalogue
    }

    override fun getById(catalogueId: String): Optional<Catalogue> {
        val catalogue = coll.find(Catalogue::id eq catalogueId).firstOrNull()
                ?: return Optional.empty()

        return Optional.of(catalogue)
    }

    override fun getAll(order: SortBy): List<Catalogue> {
        return paginator.getAll(order)
    }

    override fun getAll(order: SortBy, page: Int, pageSize: Int): List<Catalogue> {
        return paginator.getAll(order, page, pageSize)
    }

    override fun changeTitle(catalogueId: String, newTitle: String): Catalogue {
        val catalogue = findOrThrow(catalogueId)

        if (coll.find(Catalogue::title eq newTitle).firstOrNull() != null)
            throw CatalogueTitleTakenException(newTitle)

        val updatedCatalogue = catalogue.copy(title = newTitle)

        coll.update(updatedCatalogue)
        return updatedCatalogue
    }

    override fun changeParentCatalogue(childCatalogueId: String, parentCatalogue: Catalogue): Catalogue {
        val childCatalogue = findOrThrow(childCatalogueId)

        if (childCatalogue.parentId == parentCatalogue.id)
            throw CatalogueAlreadyAChildException(parentCatalogue.id, childCatalogue.id)

        if (childCatalogue.id == parentCatalogue.id)
            throw SelfContainedCatalogueException(parentCatalogue.id)

        if (parentCatalogue.parentId == childCatalogue.id)
            throw CatalogueCircularInheritanceException(parentCatalogue.id, childCatalogue.id)

        val updatedChild = childCatalogue
                .copy(parentId = parentCatalogue.id)

        parentCatalogue.childrenIds.append(childCatalogueId)

        coll.update(updatedChild)
        coll.update(parentCatalogue)
        return updatedChild
    }

    override fun delete(catalogueId: String): Catalogue {
        val catalogue = findOrThrow(catalogueId)

        catalogue.childrenIds
                .raw()
                .keys
                .forEach {
            val subCatalogue = coll.find(Catalogue::id eq it).first()
            coll.update(Catalogue::id eq it, subCatalogue.copy(parentId = catalogue.parentId))
        }

        coll.remove(Catalogue::id eq catalogueId)
        return catalogue
    }

    override fun appendChildCatalogue(parentCatalogueId: String, childCatalogue: Catalogue): Catalogue {
        if (childCatalogue.parentId == parentCatalogueId)
            throw CatalogueAlreadyAChildException(parentCatalogueId, childCatalogue.id)

        if (childCatalogue.id == parentCatalogueId)
            throw SelfContainedCatalogueException(parentCatalogueId)

        val parentCatalogue = findOrThrow(parentCatalogueId)

        if (parentCatalogue.parentId == childCatalogue.id)
            throw CatalogueCircularInheritanceException(parentCatalogueId, childCatalogue.id)

        val updatedChild = childCatalogue
                .copy(parentId = parentCatalogueId)

        parentCatalogue.childrenIds.append(childCatalogue.id)

        coll.update(updatedChild)
        coll.update(parentCatalogue)
        return updatedChild
    }

    override fun removeChildCatalogue(parentCatalogueId: String, childCatalogue: Catalogue): Catalogue {
        if (childCatalogue.parentId != parentCatalogueId)
            throw CatalogueNotAChildException(parentCatalogueId, childCatalogue.id)

        val parentCatalogue = findOrThrow(parentCatalogueId)

        val updatedChild = childCatalogue
                .copy(parentId = null)

        parentCatalogue.childrenIds.subtract(childCatalogue.id)

        coll.update(updatedChild)
        coll.update(parentCatalogue)
        return updatedChild
    }

    override fun switchChildCatalogues(parentCatalogueId: String,
                                       firstChild: Catalogue,
                                       secondChild: Catalogue): Catalogue {
        val catalogue = findOrThrow(parentCatalogueId)

        return try {
            catalogue.childrenIds.switch(firstChild.id, secondChild.id)

            coll.update(catalogue)
            catalogue
        } catch (ex: ElementNotInMapException) {
            throw CatalogueNotAChildException(parentCatalogueId, ex.reference as String)
        }
    }

    /**
     * Finds a catalogue by id or throws an exception.
     */
    private fun findOrThrow(catalogueId: String): Catalogue {
        return coll.find(Catalogue::id eq catalogueId).firstOrNull()
                ?: throw CatalogueNotFoundException(catalogueId)
    }
}