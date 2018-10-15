package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.Entries
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryNotFoundException
import com.tsbonev.nharker.core.EntryRequest
import com.tsbonev.nharker.cqrs.Command
import com.tsbonev.nharker.cqrs.CommandHandler
import com.tsbonev.nharker.cqrs.CommandResponse
import com.tsbonev.nharker.cqrs.Event
import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.cqrs.Workflow
import com.tsbonev.nharker.server.helpers.HttpStatus
import org.slf4j.LoggerFactory

/**
 * Provides the command handlers that affect entries directly
 * and the command handlers that directly query entries.
 *
 * Provides the event handlers that are concerned with the state
 * of entries.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class EntryWorkflow(private val eventBus: EventBus,
                    private val entries: Entries) : Workflow {
    private val logger = LoggerFactory.getLogger("EntryWorkflow")

    //region Command Handlers

    /**
     * Creates an entry.
     * @code 201
     * @payload The created entry.
     * @publishes EntryDeletedEvent
     */
    @CommandHandler
    fun createEntry(command: CreateEntryCommand): CommandResponse {
        val createdEntry = entries.create(command.entryRequest)

        eventBus.publish(EntryCreatedEvent(createdEntry))
        return CommandResponse(HttpStatus.Created.value, createdEntry)
    }

    /**
     * Deletes an entry.
     * @code 200
     * @payload The deleted entry.
     * @publishes EntryDeletedEvent
     *
     * If entry is not found, logs the id.
     * @code 400
     */
    @CommandHandler
    fun deleteEntry(command: DeleteEntryCommand): CommandResponse {
        return try {
            val deletedEntry = entries.delete(command.entryId)
            eventBus.publish(EntryDeletedEvent(deletedEntry))
            CommandResponse(HttpStatus.OK.value, deletedEntry)
        } catch (e: EntryNotFoundException) {
            logger.error("Failed to delete entry with id ${command.entryId}!")
            CommandResponse(HttpStatus.BadRequest.value)
        }
    }

    /**
     * Updates an entry's content.
     * @code 200
     * @payload The updated entry.
     * @publishes EntryUpdatedEvent
     *
     * If the entry is not, logs the id.
     * @code 400
     */
    @CommandHandler
    fun updateEntryContent(command: UpdateEntryContentCommand): CommandResponse {
        return try {
            val updatedEntry = entries.updateContent(command.entryId, command.content)
            eventBus.publish(EntryUpdatedEvent(updatedEntry))
            CommandResponse(HttpStatus.OK.value, updatedEntry)
        } catch (ex: EntryNotFoundException) {
            logger.error("Could not find entry with id ${command.entryId}!")
            CommandResponse(HttpStatus.BadRequest.value)
        }
    }

    /**
     * Updates an entry's links.
     * @code 200
     * @payload The updated entry.
     * @publishes EntryUpdatedEvent
     *
     * If the entry is not, logs the id.
     * @code 400
     */
    @CommandHandler
    fun updateEntryLinks(command: UpdateEntryLinksCommand): CommandResponse {
        return try {
            val updatedEntry = entries.updateLinks(command.entryId, command.entryLinks)
            eventBus.publish(EntryUpdatedEvent(updatedEntry))
            CommandResponse(HttpStatus.OK.value, updatedEntry)
        } catch (e: EntryNotFoundException) {
            logger.error("Could not find entry with id ${command.entryId}!")
            CommandResponse(HttpStatus.BadRequest.value)
        }
    }

    /**
     * Retrieves an entry by id.
     * @code 200
     * @payload The retrieved entry.
     *
     * If the entry is not, logs the id.
     * @code 404
     */
    @CommandHandler
    fun getEntryById(command: GetEntryByIdCommand): CommandResponse {
        val entry = entries.getById(command.entryId)

        return if (entry.isPresent) CommandResponse(HttpStatus.OK.value, entry.get())
        else {
            logger.error("Could not find entry with id ${command.entryId}!")
            CommandResponse(HttpStatus.NotFound.value)
        }
    }


    /**
     * Retrieves a list of entries by their content.
     * @code 200
     * @payload A list of entries whose contents match the request's search text.
     */
    @CommandHandler
    fun searchEntryContent(command: SearchEntriesByContentCommand): CommandResponse {
        val entryList = entries.getByContent(command.searchText)

        return CommandResponse(HttpStatus.OK.value, entryList)
    }
    //endregion

    //region Event Handlers

    //endregion
}

//region Queries
data class GetEntryByIdCommand(val entryId: String) : Command

data class SearchEntriesByContentCommand(val searchText: String) : Command
//endregion

//region Commands
data class CreateEntryCommand(val entryRequest: EntryRequest) : Command

data class EntryCreatedEvent(val entry: Entry) : Event

data class DeleteEntryCommand(val entryId: String) : Command
data class EntryDeletedEvent(val entry: Entry) : Event

data class UpdateEntryContentCommand(val entryId: String, val content: String) : Command
data class UpdateEntryLinksCommand(val entryId: String, val entryLinks: Map<String, String>) : Command
data class EntryUpdatedEvent(val entry: Entry) : Event
//endregion