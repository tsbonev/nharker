package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.PaginationException
import com.tsbonev.nharker.core.Paginator
import com.tsbonev.nharker.core.SortBy
import com.tsbonev.nharker.cqrs.Command
import com.tsbonev.nharker.cqrs.CommandHandler
import com.tsbonev.nharker.cqrs.CommandResponse
import com.tsbonev.nharker.cqrs.StatusCode
import com.tsbonev.nharker.cqrs.Workflow
import com.tsbonev.nharker.server.helpers.ExceptionLogger

/**
 * Provides the queries that retrieve paginated domain objects.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class PaginationWorkflow(private val paginators: Map<Class<*>, Paginator<*>>,
                         private val exceptionLogger: ExceptionLogger) : Workflow {
    //region Command Handlers
    /**
     * Retrieves all domain objects specified in the command.
     * @code 200
     * @payload A list of the objects specified.
     *
     * If no paginator is registered for the requested type, logs an error with the type.
     * @code 400
     */
    @CommandHandler
    fun getAllDomainObjects(command: GetAllDomainObjectsCommand): CommandResponse {
        return try {
            val paginator = getPaginatorForClass(command.objectType)

            val objects = paginator.getAll(command.order)

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
     *
     * If no paginator is registered for the requested type, logs an error with the type.
     * @code 400
     */
    @CommandHandler
    fun getAllDomainObjectsPaginated(command: GetPaginatedDomainObjectsCommand): CommandResponse {
        return try {
            val paginator = getPaginatorForClass(command.objectType)

            val objects = paginator.getAll(command.order, command.page, command.pageSize)

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

data class GetPaginatedDomainObjectsCommand(val order: SortBy,
                                            val page: Int,
                                            val pageSize: Int,
                                            val objectType: Class<*>) : Command

data class GetAllDomainObjectsCommand(val order: SortBy,
                                      val objectType: Class<*>) : Command

//endregion

/**
 * Exception thrown when a paginator is not found.
 */
class NoPaginatorRegisteredException(val objectType: Class<*>) : Throwable()