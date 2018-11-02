package com.tsbonev.nharker.core

import java.util.Optional

/**
 * Provides the methods to handle catalogues in persistence.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface Catalogues {
	/**
	 * Creates and returns a catalogue.
	 *
	 * @param catalogueRequest The requested catalogue.
	 * @return The created catalogue.
	 *
	 * @exception CatalogueNotFoundException thrown when the catalogue is not found.
	 * @exception CatalogueTitleTakenException thrown when the catalogue's title is already taken.
	 */
	@Throws(
		CatalogueTitleTakenException::class,
		CatalogueNotFoundException::class
	)
	fun create(catalogueRequest: CatalogueRequest): Catalogue

	/**
	 * Saves a catalogue into persistence, overwriting the previous one
	 * if it exists.
	 *
	 * @param catalogue The catalogue to save.
	 * @return The saved catalogue.
	 */
	fun save(catalogue: Catalogue): Catalogue

	/**
	 * Retrieves an article by id.
	 *
	 * @param catalogueId The id to search for.
	 * @return An optional article.
	 */
	fun getById(catalogueId: String): Optional<Catalogue>

	/**
	 * Deletes a catalogue by id.
	 *
	 * @param catalogueId The id of the catalogue targeted.
	 * @return The deleted catalogue.
	 *
	 * @exception CatalogueNotFoundException thrown when the catalogue is not found.
	 */
	@Throws(CatalogueNotFoundException::class)
	fun delete(catalogueId: String): Catalogue

	/**
	 * Changes the title of a catalogue.
	 *
	 * @param catalogueId The id of the catalogue targeted.
	 * @param newTitle The new title.
	 * @return The updated catalogue.
	 *
	 * @exception CatalogueNotFoundException thrown when the catalogue is not found.
	 * @exception CatalogueTitleTakenException thrown when the catalogue's title is already taken.
	 */
	@Throws(
		CatalogueNotFoundException::class,
		CatalogueTitleTakenException::class
	)
	fun changeTitle(catalogueId: String, newTitle: String): Catalogue

	/**
	 * Changes the parent of a catalogue.
	 *
	 * @param childCatalogueId The id of the catalogue targeted.
	 * @param parentCatalogue The new parent.
	 * @return The updated child catalogue.
	 *
	 * @exception CatalogueNotFoundException thrown when the child catalogue is not found.
	 * @exception CatalogueAlreadyAChildException thrown when the child catalogue is already a child.
	 * @exception SelfContainedCatalogueException thrown when the parent catalogue would become its own parent.
	 * @exception CatalogueCircularInheritanceException thrown when the child is
	 * the parent of the requested parent.
	 */
	@Throws(
		CatalogueNotFoundException::class,
		CatalogueAlreadyAChildException::class,
		SelfContainedCatalogueException::class,
		CatalogueCircularInheritanceException::class
	)
	fun changeParentCatalogue(childCatalogueId: String, parentCatalogue: Catalogue): Catalogue

	/**
	 * Orphans a catalogue by setting its parentId to null.
	 *
	 * @param catalogueId The catalogue to orphan.
	 * @return The updated parent catalogue.
	 *
	 * @exception CatalogueNotFoundException thrown when the catalogue is not found.
	 */
	@Throws(CatalogueNotFoundException::class)
	fun orphanCatalogue(catalogueId: String): Catalogue

	/**
	 * Switches the order of two children catalogues in a parent catalogue.
	 *
	 * @param parentCatalogueId The id of the catalogue targeted.
	 * @param firstChild The first catalogue.
	 * @param secondChild The second catalogue.
	 * @return The updated catalogue.
	 *
	 * @exception CatalogueNotFoundException thrown when the catalogue is not found
	 * @exception CatalogueNotAChildException thrown when the catalogue is not a child.
	 */
	@Throws(
		CatalogueNotFoundException::class,
		CatalogueNotAChildException::class
	)
	fun switchChildCatalogues(parentCatalogueId: String, firstChild: Catalogue, secondChild: Catalogue): Catalogue
}