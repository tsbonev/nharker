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
	 *
	 * @exception ArticleTitleTakenException thrown when the article title is taken.
	 */
	@Throws(ArticleTitleTakenException::class)
	fun create(articleRequest: ArticleRequest): Article

	/**
	 * Saves an article, overwriting the previous one if it exists.
	 *
	 * @param article The article to save.
	 */
	fun save(article: Article): Article

	/**
	 * Deletes an article by id.
	 *
	 * @param articleId The id of the article to delete.
	 * @return The deleted article.
	 *
	 * @exception ArticleNotFoundException thrown when the article is not found.
	 */
	@Throws(ArticleNotFoundException::class)
	fun deleteById(articleId: String): Article

	/**
	 * Changes the title of an article.
	 *
	 * @param articleId The id of the article to rename.
	 * @param newTitle The new title.
	 * @return The updated article.
	 *
	 * @exception ArticleNotFoundException thrown when the article is not found.
	 * @exception ArticleTitleTakenException thrown when the article title is taken.
	 */
	@Throws(
		ArticleNotFoundException::class,
		ArticleTitleTakenException::class
	)
	fun changeTitle(articleId: String, newTitle: String): Article

	/**
	 * Retrieves an article by id.
	 *
	 * @param articleId The id of the article sought.
	 * @return An optional article.
	 */
	fun getById(articleId: String): Optional<Article>

	/**
	 * Retrieves all stored articles.
	 *
	 * @param order The order in which to sort.
	 * @return A list of all stored articles.
	 */
	fun getAll(order: SortBy): List<Article>

	/**
	 * Retrieves all stored articles, paginated.
	 *
	 * @param order The order in which to sort.
	 * @param page The index of the page.
	 * @param pageSize The size of the page.
	 * @return A list of articles, paginated.
	 *
	 * @exception ArticlePaginationException thrown when the pageSize
	 * and page count are 0 or less than 1 respectively.
	 */
	@Throws(ArticlePaginationException::class)
	fun getPaginated(order: SortBy, page: Int, pageSize: Int): List<Article>

	/**
	 * Retrieves a list of articles that have a given catalogue's id
	 * in their catalogues list.
	 *
	 * @param catalogue The catalogue sought.
	 * @return A list of articles.
	 */
	fun getByCatalogue(catalogue: Catalogue): List<Article>

	/**
	 * Adds a catalogue's id to an article's catalogues list.
	 *
	 * @param articleId The id of the article.
	 * @param catalogue The catalogue to add.
	 *
	 * @return The updated article.
	 *
	 * @exception ArticleNotFoundException thrown when the article is not found.
	 */
	@Throws(ArticleNotFoundException::class)
	fun addCatalogue(articleId: String, catalogue: Catalogue): Article

	/**
	 * Removes a catalogue's id from an article's catalogues list.
	 *
	 * @param articleId The id of the article.
	 * @param catalogue The catalogue to remove.
	 *
	 * @return The updated article.
	 *
	 * @exception ArticleNotFoundException thrown when the article is not found.
	 */
	@Throws(ArticleNotFoundException::class)
	fun removeCatalogue(articleId: String, catalogue: Catalogue): Article

	/**
	 * Appends an entry to an article.
	 *
	 * @param articleId The id of the article targeted.
	 * @param entry The entry to append.
	 * @return The updated article.
	 *
	 * @exception ArticleNotFoundException thrown when the article is not found.
	 * @exception EntryAlreadyInArticleException thrown when the entry is already in the article.
	 */
	@Throws(
		ArticleNotFoundException::class,
		EntryAlreadyInArticleException::class
	)
	fun appendEntry(articleId: String, entry: Entry): Article

	/**
	 * Removes an entry from an article.
	 *
	 * @param articleId The id of the article targeted.
	 * @param entry The entry to remove.
	 * @return The updated article.
	 *
	 * @exception ArticleNotFoundException thrown when the article is not found.
	 * @exception EntryNotInArticleException thrown when the entry is not in the article.
	 */
	@Throws(
		ArticleNotFoundException::class,
		EntryNotInArticleException::class
	)
	fun removeEntry(articleId: String, entry: Entry): Article

	/**
	 * Switches two entries in an article.
	 *
	 * @param articleId The id of the article.
	 * @param first The first entry.
	 * @param second The second entry.
	 * @return The updated article.
	 *
	 * @exception ArticleNotFoundException thrown when the article is not found.
	 * @exception EntryNotInArticleException thrown when one or both of the
	 * entries are not in the article.
	 */
	@Throws(
		ArticleNotFoundException::class,
		EntryNotInArticleException::class
	)
	fun switchEntries(articleId: String, first: Entry, second: Entry): Article

	/**
	 * Attaches a property to an article, if another property
	 * has been attached, replaces the entry it points to.
	 *
	 * @param articleId: The id of the targeted article.
	 * @param propertyName The name of the property.
	 * @param propertyEntry The entry describing the property.
	 * @return The updated article.
	 *
	 * @exception ArticleNotFoundException thrown when the article is not found.
	 */
	@Throws(ArticleNotFoundException::class)
	fun attachProperty(articleId: String, propertyName: String, propertyEntry: Entry): Article

	/**
	 * Detaches a property from an article.
	 *
	 * @param articleId The id of the targeted article.
	 * @param propertyName The name of the property.
	 * @return The updated article and the removed property id in a pair.
	 *
	 * @exception ArticleNotFoundException thrown when the article is not found.
	 * @exception PropertyNotFoundException thrown when there is no property of the given name.
	 */
	@Throws(
		ArticleNotFoundException::class,
		PropertyNotFoundException::class
	)
	fun detachProperty(articleId: String, propertyName: String): Pair<Article, String>

	/**
	 * Returns a list of all articles that match a give title.
	 *
	 * @param searchString The string to search by.
	 * @return A list of articles.
	 */
	fun searchByFullTitle(searchString: String): List<Article>
}