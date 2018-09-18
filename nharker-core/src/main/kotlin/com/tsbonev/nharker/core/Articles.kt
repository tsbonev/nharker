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
     * Saves an article into persistence, overwriting the previous one
     * if it exists.
     */
    fun save(article: Article): Article

    /**
     * Retrieves an optional article by value.
     *
     * @param articleId The value of the article sought.
     * @return An optional article.
     */
    fun getById(articleId: String): Optional<Article>

    /**
     * Appends an entry to an article.
     *
     * @param articleId The value of the article targeted.
     * @param entry The entry to append.
     * @return The updated entry.
     */
    @Throws(ArticleNotFoundException::class,
            EntryAlreadyInArticleException::class)
    fun appendEntry(articleId: String, entry: Entry): Entry

    /**
     * Removes an entry from an article, deleting it from persistence.
     *
     * @param articleId The value of the article targeted.
     * @param entry The entry to remove.
     * @return The removed entry.
     */
    @Throws(ArticleNotFoundException::class,
            EntryNotInArticleException::class)
    fun removeEntry(articleId: String, entry: Entry): Entry

    /**
     * Sets the catalogue of an article.
     *
     * @param articleId The value of the article targeted.
     * @param catalogue The catalogue.
     * @return The updated catalogue.
     */
    @Throws(ArticleNotFoundException::class,
            ArticleAlreadyInCatalogueException::class)
    fun setCatalogue(articleId: String, catalogue: Catalogue): Catalogue

    /**
     * Switches two entries in the article.
     *
     * @param articleId The value of the article.
     * @param first The first entry.
     * @param second The second entry.
     * @return The updated article.
     */
    @Throws(ArticleNotFoundException::class,
            EntryNotInArticleException::class)
    fun switchEntries(articleId: String, first: Entry, second: Entry): Article
}