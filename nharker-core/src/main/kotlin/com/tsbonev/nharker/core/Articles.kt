package com.tsbonev.nharker.core

import java.util.Optional

/**
 * Provides the methods to handle articles in persistence.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface Articles {
    /**
     * Creates an article from a request and saves it.
     *
     * @param articleRequest The article to be created.
     * @return The created article.
     */
    @Throws(ArticleTitleTakenException::class)
    fun create(articleRequest: ArticleRequest): Article

    /**
     * Saves an article, overwriting the previous one if it exists.
     */
    fun save(article: Article): Article

    /**
     * Deletes an article.
     *
     * @param articleId The id of the article to delete.
     * @return The deleted article.
     */
    @Throws(ArticleNotFoundException::class)
    fun delete(articleId: String): Article

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
     * Switches two entries in an article.
     *
     * @param articleId The value of the article.
     * @param first The first entry.
     * @param second The second entry.
     * @return The updated article.
     */
    @Throws(ArticleNotFoundException::class,
            EntryNotInArticleException::class)
    fun switchEntries(articleId: String, first: Entry, second: Entry): Article

    /**
     * Attaches a property to an article, if another property
     * has been attached, replaces the entry it points to.
     *
     * @param articleId: The id of the targeted article.
     * @param propertyName The name of the property.
     * @param property The entry describing the property.
     * @return The attached property.
     */
    @Throws(ArticleNotFoundException::class)
    fun attachProperty(articleId: String, propertyName: String, property: Entry): Entry

    /**
     * Detaches a property from an article.
     *
     * @param articleId The id of the targeted article.
     * @param propertyName The name of the property.
     * @return The removed property.
     */
    @Throws(ArticleNotFoundException::class,
            PropertyNotFoundException::class)
    fun detachProperty(articleId: String, propertyName: String): Entry

    /**
     * Returns an article with a given link title.
     *
     * @param linkTitle The link title to search for.
     * @return An optional article.
     */
    fun getByLinkTitle(linkTitle: String): Optional<Article>

    /**
     * Returns a list of all articles that match a give title.
     *
     * @param searchString The string to search by.
     * @return A list of articles.
     */
    fun searchByFullTitle(searchString: String): List<Article>

    /**
     * Returns a list of all article titles that match a set of
     * link titles.
     *
     * @param linkTitleList The links to match to.
     * @return A list of full article titles.
     */
    fun getArticleTitles(linkTitleList: Set<String>): List<String>
}