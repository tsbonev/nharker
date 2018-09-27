package com.tsbonev.nharker.core

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
     * Saves a catalogue into persistence, overwriting the previous one
     * if it exists.
     */
    fun save(catalogue: Catalogue): Catalogue

    /**
     * Retrieves an article by value.
     *
     * @param catalogueId The value search by.
     * @return An optional article.
     */
    fun getById(catalogueId: String): Optional<Catalogue>

    /**
     * Changes the title of a catalogue.
     *
     * @param catalogueId The value of the catalogue targeted.
     * @param newTitle The new title.
     * @return The updated catalogue.
     */
    @Throws(CatalogueNotFoundException::class,
            CatalogueTitleTakenException::class)
    fun changeTitle(catalogueId: String, newTitle: String): Catalogue

    /**
     * Changes the parent of a catalogue.
     *
     * @param childCatalogueId The id of the catalogue targeted.
     * @param parentCatalogue The id of the new parent.
     * @return The updated child catalogue.
     */
    @Throws(CatalogueNotFoundException::class,
            CatalogueAlreadyAChildException::class)
    fun changeParentCatalogue(childCatalogueId: String, parentCatalogue: Catalogue): Catalogue

    /**
     * Deletes a catalogue by value.
     *
     * @param catalogueId The value of the catalogue targeted.
     * @return The deleted catalogue.
     */
    @Throws(CatalogueNotFoundException::class)
    fun delete(catalogueId: String): Catalogue

    /**
     * Appends a catalogue to the targeted catalogue's children list.
     *
     * @param parentCatalogueId The id of the targeted parent catalogue.
     * @param childCatalogue The id of the child catalogue.
     * @return The updated parent catalogue.
     */
    @Throws(CatalogueNotFoundException::class,
            CatalogueAlreadyAChildException::class,
            SelfContainedCatalogueException::class)
    fun appendSubCatalogue(parentCatalogueId: String, childCatalogue: Catalogue): Catalogue

    /**
     * Removes a catalogue from the targeted catalogue's children list.
     *
     * @param parentCatalogueId The id of the parent catalogue.
     * @param childCatalogue The id of the child catalogue.
     * @return The updated parent catalogue.
     */
    @Throws(CatalogueNotFoundException::class,
            CatalogueNotAChildException::class)
    fun removeSubCatalogue(parentCatalogueId: String, childCatalogue: Catalogue): Catalogue

    /**
     * Appends an article from a catalogue.
     *
     * @param parentCatalogueId The value of the catalogue targeted.
     * @param article The article.
     * @return The appended article.
     */
    @Throws(CatalogueNotFoundException::class,
            ArticleAlreadyInCatalogueException::class)
    fun appendArticle(parentCatalogueId: String, article: Article): Article

    /**
     * Removes an article from a catalogue.
     *
     * @param parentCatalogueId The value of the catalogue targeted.
     * @param article The  article.
     * @return The removed article.
     */
    @Throws(CatalogueNotFoundException::class,
            ArticleNotInCatalogueException::class)
    fun removeArticle(parentCatalogueId: String, article: Article): Article

    /**
     * Switches the order of two articles in a catalogue.
     *
     * @param catalogueId The value of the catalogue targeted.
     * @param first The first article.
     * @param second The second article.
     * @return The updated catalogue.
     */
    @Throws(CatalogueNotFoundException::class,
            ArticleNotInCatalogueException::class)
    fun switchArticles(catalogueId: String, first: Article, second: Article): Catalogue

    /**
     * Switches the order of two subcatalogues in a catalogue.
     *
     * @param parentCatalogueId The value of the catalogue targeted.
     * @param firstChild The first catalogue.
     * @param secondChild The second catalogue.
     * @return The updated catalogue.
     */
    @Throws(CatalogueNotFoundException::class,
            CatalogueNotAChildException::class)
    fun switchSubCatalogues(parentCatalogueId: String, firstChild: Catalogue, secondChild: Catalogue): Catalogue
}