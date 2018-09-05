package com.tsbonev.nharker.core

import com.tsbonev.nharker.core.exceptions.ArticleAlreadyInCatalogueException
import com.tsbonev.nharker.core.exceptions.ArticleNotFoundException
import com.tsbonev.nharker.core.exceptions.CatalogueNotFoundException
import java.util.Optional

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface ArticleService {
    /**
     * Retrieves an optional article by id.
     *
     * @param articleId The id of the article sought.
     * @return An optional article.
     */
    fun getById(articleId: String): Optional<Article>

    /**
     * Sets the catalogueId of an article.
     *
     * @param articleId The id of the article targeted.
     * @param catalogueId The id of the catalogue.
     * @return The new catalogue of the article.
     */
    @Throws(ArticleNotFoundException::class,
            CatalogueNotFoundException::class,
            ArticleAlreadyInCatalogueException::class)
    fun setCatalogue(articleId: String, catalogueId: String): Catalogue
}