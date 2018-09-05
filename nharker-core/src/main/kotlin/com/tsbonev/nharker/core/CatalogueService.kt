package com.tsbonev.nharker.core

import java.util.Optional

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface CatalogueService {
    /**
     * Retrieves an article by id.
     *
     * @param catalogueId The id search by.
     * @return An optional article.
     */
    fun getById(catalogueId: String): Optional<Catalogue>
}