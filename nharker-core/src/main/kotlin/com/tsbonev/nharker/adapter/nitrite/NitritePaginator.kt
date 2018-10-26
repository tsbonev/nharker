package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.PaginationException
import com.tsbonev.nharker.core.Paginator
import com.tsbonev.nharker.core.SortBy
import org.dizitart.no2.FindOptions
import org.dizitart.no2.SortOrder
import org.dizitart.no2.objects.ObjectRepository

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitritePaginator<T>(private val repo: ObjectRepository<T>) : Paginator<T> {
	override fun getAll(order: SortBy): List<T> {
		val sortOrder = if (order == SortBy.ASCENDING) SortOrder.Ascending
		else SortOrder.Descending

		return repo.find(FindOptions.sort("creationDate", sortOrder)).toList()
	}

	override fun getPaginated(order: SortBy, page: Int, pageSize: Int): List<T> {
		val sortOrder = if (order == SortBy.ASCENDING) SortOrder.Ascending
		else SortOrder.Descending

		if (page < 1 || pageSize < 0) throw PaginationException(page, pageSize)

		val pageOffset = (page - 1) * pageSize

		if (repo.find().count() < pageOffset) return emptyList()

		return repo.find(
			FindOptions.sort("creationDate", sortOrder)
				.thenLimit(pageOffset, pageSize)
		).toList()
	}
}