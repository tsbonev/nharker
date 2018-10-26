package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.PaginationException
import com.tsbonev.nharker.core.Paginator
import com.tsbonev.nharker.core.SortBy
import com.tsbonev.nharker.cqrs.CommandHandler
import com.tsbonev.nharker.cqrs.CommandResponse
import com.tsbonev.nharker.cqrs.Query
import com.tsbonev.nharker.cqrs.QueryResponse
import com.tsbonev.nharker.cqrs.StatusCode
import com.tsbonev.nharker.cqrs.Workflow
import com.tsbonev.nharker.server.helpers.ExceptionLogger

/**
 * Provides the queries that retrieve paginated domain objects.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class PaginationWorkflow(
	private val paginators: Map<Class<*>, Paginator<*>>,
	private val exceptionLogger: ExceptionLogger
) : Workflow {
	//region Command Handlers
	/**
	 * Retrieves all domain objects specified in the query.
	 * @code 200
	 * @payload A list of the objects specified.
	 *
	 * If no paginator is registered for the requested type, logs an error with the type.
	 * @code 400
	 * @exception NoPaginatorRegisteredException
	 */
	@CommandHandler
	fun getAllDomainObjects(query: GetAllDomainObjectsQuery): QueryResponse {
		return try {
			val paginator = getPaginatorForClass(query.objectType)

			val objects = paginator.getAll(query.order)
			CommandResponse(StatusCode.OK, objects)
		} catch (e: NoPaginatorRegisteredException) {
			exceptionLogger.logException(e)
		}
	}

	/**
	 * Retrieves all domain objects specified in the command and paginates them.
	 * @code 200
	 * @payload A list of the objects specified.
	 *
	 * If an illegal page size or page index are passed, logs an error with the size and index.
	 * @code 400
	 * @exception PaginationException
	 *
	 * If no paginator is registered for the requested type, logs an error with the type.
	 * @code 400
	 * @exception NoPaginatorRegisteredException
	 */
	@CommandHandler
	fun getAllDomainObjectsPaginated(query: GetPaginatedDomainObjectsQuery): QueryResponse {
		return try {
			val paginator = getPaginatorForClass(query.objectType)

			val objects = paginator.getPaginated(query.order, query.page, query.pageSize)
			CommandResponse(StatusCode.OK, objects)
		} catch (e: PaginationException) {
			exceptionLogger.logException(e)
		} catch (e: NoPaginatorRegisteredException) {
			exceptionLogger.logException(e)
		}
	}
	//endregion

	/**
	 * Returns the correct paginator for a given class.
	 */
	private fun getPaginatorForClass(objectType: Class<*>): Paginator<*> {
		return paginators[objectType] ?: throw NoPaginatorRegisteredException(objectType)
	}
}

//region Queries
data class GetPaginatedDomainObjectsQuery(
	val order: SortBy,
	val page: Int,
	val pageSize: Int,
	val objectType: Class<*>
) : Query

data class GetAllDomainObjectsQuery(
	val order: SortBy,
	val objectType: Class<*>
) : Query
//endregion

/**
 * Exception thrown when a paginator is not found.
 */
class NoPaginatorRegisteredException(val objectType: Class<*>) : Throwable()