package com.tsbonev.nharker.core

import com.tsbonev.nharker.core.exceptions.*
import java.util.Optional

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface Articles {

    /**
     * Creates an article from a request and saves it into persistence.
     *
     * @param articleRequest The article to be created.
     * @return The created article.
     */
    @Throws(ArticleTitleTakenException::class)
    fun create(articleRequest: ArticleRequest) : Article

    /**
     * Retrieves an optional article by id.
     *
     * @param articleId The id of the article sought.
     * @return An optional article.
     */
    fun getById(articleId: String): Optional<Article>

    /**
     * Appends an entry to an article, first creating that entry and automatically
     * linking it to other articles.
     *
     * @param articleId The id of the article targeted.
     * @param entryId The id of the entry.
     * @return The appended entry.
     */
    @Throws(ArticleNotFoundException::class,
            EntryNotFoundException::class,
            EntryAlreadyInArticleException::class)
    fun appendEntry(articleId: String, entryId: String): Entry

    /**
     * Removes an entry from an article, deleting it from persistence.
     *
     * @param articleId The id of the article targeted.
     * @param entryId The id of the entry targeted.
     * @return The removed entry.
     */
    @Throws(ArticleNotFoundException::class,
            EntryNotFoundException::class,
            EntryNotInArticleException::class)
    fun removeEntry(articleId: String, entryId: String): Entry

    /**
     * Sets the catalogueId of an article.
     *
     * @param articleId The id of the article targeted.
     * @param catalogueId The id of the catalogue.
     * @return The updated article.
     */
    @Throws(ArticleNotFoundException::class,
            CatalogueNotFoundException::class,
            ArticleAlreadyInCatalogueException::class)
    fun setCatalogue(articleId: String, catalogueId: String): Article
}