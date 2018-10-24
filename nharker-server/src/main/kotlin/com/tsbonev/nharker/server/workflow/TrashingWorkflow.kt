package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.EntityCannotBeCastException
import com.tsbonev.nharker.core.EntityNotInTrashException
import com.tsbonev.nharker.core.TrashCollector
import com.tsbonev.nharker.cqrs.Command
import com.tsbonev.nharker.cqrs.CommandHandler
import com.tsbonev.nharker.cqrs.CommandResponse
import com.tsbonev.nharker.cqrs.Event
import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.cqrs.EventHandler
import com.tsbonev.nharker.cqrs.Query
import com.tsbonev.nharker.cqrs.QueryResponse
import com.tsbonev.nharker.cqrs.StatusCode
import com.tsbonev.nharker.cqrs.Workflow
import com.tsbonev.nharker.server.helpers.ExceptionLogger

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class TrashingWorkflow(private val eventBus: EventBus,
                       private val trashCollector: TrashCollector,
                       private val exceptionLogger: ExceptionLogger) : Workflow {
    //region Command Handlers
    /**
     * Restores an entity from the trash.
     * @code 200
     * @payload The restored entity.
     * @publishes EntityRestoredEvent.
     *
     * If the entity is not found in the class, logs the id and class.
     * @code 404
     * @exception EntityNotInTrashException
     *
     * If the entity cannot be cast to the specified class, logs the id and class.
     * @code 400
     * @exception EntityCannotBeCastException
     */
    @CommandHandler
    fun restoreEntity(command: RestoreTrashedEntityCommand): CommandResponse {
        return try {
            val trashedEntity = trashCollector.restore(command.entityId, command.entityClass)

            eventBus.publish(EntityRestoredEvent(trashedEntity, command.entityClass))

            CommandResponse(StatusCode.OK, command.entityClass.cast(trashedEntity))
        } catch (e: EntityNotInTrashException) {
            exceptionLogger.logException(e)
        } catch (e: EntityCannotBeCastException) {
            exceptionLogger.logException(e)
        }
    }

    /**
     * Clears the trash store.
     * @code 200
     * @payload The cleared trashed entities.
     * @publishes TrashStoreClearedEvent
     */
    @Suppress("UNUSED_PARAMETER")
    @CommandHandler
    fun clearTrashStore(command: ClearTrashStoreCommand): CommandResponse {
        val trashedEntities = trashCollector.view()
        trashCollector.clear()

        eventBus.publish(TrashStoreClearedEvent(trashedEntities))

        return CommandResponse(StatusCode.OK, trashedEntities)
    }

    /**
     * Returns a list of trashed entities from a specified class.
     * @code 200
     * @payload A list of entities of the specified class.
     */
    @CommandHandler
    fun viewTrashedEntities(query: ViewTrashedEntitiesQuery): QueryResponse {
        val entityList = mutableListOf<Any>()

        trashCollector
                .view()
                .asSequence()
                .filter {
                    query.entityClass.isInstance(it)
                }
                .mapTo(entityList) {
                    query.entityClass.cast(it)
                }

        return CommandResponse(StatusCode.OK, entityList)
    }
    //region

    //region Event Handlers
    /**
     * Stores a deleted entry in the trash.
     */
    @EventHandler
    fun onEntryDeleted(event: EntryDeletedEvent) {
        trashCollector.trash(event.entry)
    }

    /**
     * Stores a deleted article in the trash.
     */
    @EventHandler
    fun onArticleDeleted(event: ArticleDeletedEvent) {
        trashCollector.trash(event.article)
    }

    /**
     * Stores a deleted catalogue in the trash.
     */
    @EventHandler
    fun onCatalogueDeleted(event: CatalogueDeletedEvent) {
        trashCollector.trash(event.catalogue)
    }
    //endregion
}

//region Queries
data class ViewTrashedEntitiesQuery(val entityClass: Class<*>) : Query
//endregion

//region Commands
data class RestoreTrashedEntityCommand(val entityId: String, val entityClass: Class<*>) : Command

data class EntityRestoredEvent(val entity: Any, val entityClass: Class<*>) : Event

class ClearTrashStoreCommand : Command
data class TrashStoreClearedEvent(val deletedEntities: List<Any>) : Event
//endregion