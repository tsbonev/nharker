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
import com.tsbonev.nharker.core.SelfContainedCatalogueException
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
class NitriteCatalogues(
	private val nitriteDb: Nitrite,
	private val collectionName: String = "Catalogues",
	private val clock: Clock = Clock.systemUTC()
) : Catalogues {

	private val repo: ObjectRepository<Catalogue>
		get() = nitriteDb.getRepository(collectionName, Catalogue::class.java)

	override fun create(catalogueRequest: CatalogueRequest): Catalogue {
		if (repo.find(Catalogue::title eq catalogueRequest.title).any())
			throw CatalogueTitleTakenException(catalogueRequest.title)

		if (catalogueRequest.parentId != null
			&& repo.find(Catalogue::title eq catalogueRequest.parentId).none()
		)
			throw CatalogueNotFoundException(catalogueRequest.parentId)

		val catalogue = Catalogue(
			NitriteId.newId().toString(),
			catalogueRequest.title,
			LocalDateTime.now(clock),
			parentId = catalogueRequest.parentId
		)

		repo.insert(catalogue)
		return catalogue
	}

	override fun save(catalogue: Catalogue): Catalogue {
		repo.update(catalogue, true)
		return catalogue
	}

	override fun getById(catalogueId: String): Optional<Catalogue> {
		val catalogue = repo.find(Catalogue::id eq catalogueId).firstOrNull()
			?: return Optional.empty()

		return Optional.of(catalogue)
	}

	override fun changeTitle(catalogueId: String, newTitle: String): Catalogue {
		val catalogue = findOrThrow(catalogueId)

		if (repo.find(Catalogue::title eq newTitle).any())
			throw CatalogueTitleTakenException(newTitle)

		val updatedCatalogue = catalogue.copy(title = newTitle)

		repo.update(updatedCatalogue)
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

		parentCatalogue.children.append(childCatalogueId)

		repo.update(updatedChild)
		repo.update(parentCatalogue)
		return updatedChild
	}

	override fun delete(catalogueId: String): Catalogue {
		val catalogue = findOrThrow(catalogueId)

		catalogue.children
			.raw()
			.keys
			.forEach {
				repo.find(Catalogue::id eq it).firstOrNull()?.let { child ->
					repo.update(Catalogue::id eq it, child.copy(parentId = catalogue.parentId))
				}
			}

		repo.remove(Catalogue::id eq catalogueId)
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

		parentCatalogue.children.append(childCatalogue.id)

		repo.update(updatedChild)
		repo.update(parentCatalogue)
		return updatedChild
	}

	override fun removeChildCatalogue(parentCatalogueId: String, childCatalogue: Catalogue): Catalogue {
		if (childCatalogue.parentId != parentCatalogueId)
			throw CatalogueNotAChildException(parentCatalogueId, childCatalogue.id)

		val parentCatalogue = findOrThrow(parentCatalogueId)

		val updatedChild = childCatalogue
			.copy(parentId = null)

		parentCatalogue.children.subtract(childCatalogue.id)

		repo.update(updatedChild)
		repo.update(parentCatalogue)
		return updatedChild
	}

	override fun switchChildCatalogues(
		parentCatalogueId: String,
		firstChild: Catalogue,
		secondChild: Catalogue
	): Catalogue {
		val catalogue = findOrThrow(parentCatalogueId)

		return try {
			catalogue.children.switch(firstChild.id, secondChild.id)

			repo.update(catalogue)
			catalogue
		} catch (ex: ElementNotInMapException) {
			throw CatalogueNotAChildException(parentCatalogueId, ex.reference)
		}
	}

	/**
	 * Finds a catalogue by id or throws an exception.
	 *
	 * @param catalogueId The id to find.
	 * @return The found Catalogue.
	 *
	 * @exception CatalogueNotAChildException thrown when the catalogue is not found.
	 */
	private fun findOrThrow(catalogueId: String): Catalogue {
		return repo.find(Catalogue::id eq catalogueId).firstOrNull()
			?: throw CatalogueNotFoundException(catalogueId)
	}
}