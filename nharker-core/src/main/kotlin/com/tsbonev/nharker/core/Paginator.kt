package com.tsbonev.nharker.core

/**
 * Provides the methods to paginate domain objects.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */

interface Paginator<T> {
	/**
	 * Retrieves all stored domain objects.
	 *
	 * @param order The order in which to sort.
	 * @return A list of all stored articles.
	 */
	fun getAll(order: SortBy): List<T>

	/**
	 * Retrieves all stored domain objects, paginated.
	 *
	 * @param order The order in which to sort.
	 * @param page The index of the page.
	 * @param pageSize The size of the page.
	 * @return A list of domain objects, paginated.
	 *
	 * @exception PaginationException thrown when the pageSize
	 * and page count are 0 or less than 1 respectively.
	 */
	@Throws(PaginationException::class)
	fun getPaginated(order: SortBy, page: Int, pageSize: Int): List<T>
}

enum class SortBy {
	ASCENDING,
	DESCENDING
}

class PaginationException(val page: Int, val pageSize: Int) : Throwable()