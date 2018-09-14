package com.tsbonev.nharker.core

import com.tsbonev.nharker.core.exceptions.ArticleNotFoundException
import com.tsbonev.nharker.core.exceptions.CatalogueIsAlreadyAChildException
import com.tsbonev.nharker.core.exceptions.CatalogueNotFoundException
import com.tsbonev.nharker.core.exceptions.CatalogueTitleTakenException
import java.util.Optional

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface Catalogues {
    /**
     * Creates and returns a catalogue.
     *
     * @param catalogueRequest The requested catalogue.
     * @return The created catalogue.
     */
    @Throws(CatalogueTitleTakenException::class)
    fun create(catalogueRequest: CatalogueRequest): Catalogue

    /**
     * Retrieves an article by id.
     *
     * @param catalogueId The id search by.
     * @return An optional article.
     */
    fun getById(catalogueId: String): Optional<Catalogue>

    /**
     * Changes the title of a catalogue.
     *
     * @param catalogueId The id of the catalogue targeted.
     * @param newTitle The new title.
     * @return The updated catalogue.
     */
    @Throws(CatalogueNotFoundException::class,
            CatalogueTitleTakenException::class)
    fun changeTitle(catalogueId: String, newTitle: String): Catalogue

    /**
     * Changes the parent of a catalogue.
     *
     * @param catalogueId The id of the catalogue targeted.
     * @param parentCatalogueId The id of the new parent.
     * @return The new parent catalogue and the updated child catalogue.
     */
    @Throws(CatalogueNotFoundException::class,
            CatalogueIsAlreadyAChildException::class)
    fun changeParentCatalogue(catalogueId: String, parentCatalogueId: String): Pair<Catalogue, Catalogue>

    /**
     * Deletes a catalogue by id.
     *
     * @param catalogueId The id of the catalogue targeted.
     * @return The deleted catalogue.
     */
    @Throws(CatalogueNotFoundException::class)
    fun delete(catalogueId: String): Catalogue

    /**
     * Appends a catalogue to the targeted catalogue's children list.
     *
     * @param parentCatalogueId The id of the parent catalogue.
     * @param subCatalogueId The id of the child catalogue.
     * @return The appended catalogue.
     */
    @Throws(CatalogueNotFoundException::class)
    fun appendSubcatalogue(parentCatalogueId: String, subCatalogueId: String): Catalogue

    /**
     * Removes a catalogue from the targeted catalogue's children list.
     *
     * @param parentCatalogueId The id of the parent catalogue.
     * @param subCatalogueId The id of the child catalogue.
     * @return The removed catalogue.
     */
    @Throws(CatalogueNotFoundException::class)
    fun removeSubCatalogue(parentCatalogueId: String, subCatalogueId: String): Catalogue

    /**
     * Appends an article from a catalogue.
     *
     * @param catalogueId The id of the catalogue targeted.
     * @param article The article.
     * @return The appended article.
     */
    @Throws(CatalogueNotFoundException::class)
    fun appendArticle(catalogueId: String, article: Article): Article

    /**
     * Removes an article from a catalogue.
     *
     * @param catalogueId The id of the catalogue targeted.
     * @param article The  article.
     * @return The removed article.
     */
    @Throws(CatalogueNotFoundException::class)
    fun removeArticle(catalogueId: String, article: Article): Article
}