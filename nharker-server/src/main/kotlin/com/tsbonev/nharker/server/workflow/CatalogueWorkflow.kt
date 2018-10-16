package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleAlreadyInCatalogueException
import com.tsbonev.nharker.core.ArticleNotInCatalogueException
import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.core.CatalogueAlreadyAChildException
import com.tsbonev.nharker.core.CatalogueNotAChildException
import com.tsbonev.nharker.core.CatalogueNotFoundException
import com.tsbonev.nharker.core.CatalogueRequest
import com.tsbonev.nharker.core.CatalogueTitleTakenException
import com.tsbonev.nharker.core.Catalogues
import com.tsbonev.nharker.core.SelfContainedCatalogueException
import com.tsbonev.nharker.cqrs.Command
import com.tsbonev.nharker.cqrs.CommandHandler
import com.tsbonev.nharker.cqrs.CommandResponse
import com.tsbonev.nharker.cqrs.Event
import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.cqrs.StatusCode
import com.tsbonev.nharker.cqrs.Workflow
import com.tsbonev.nharker.server.helpers.ExceptionLogger

/**
 * Provides the command handlers that affect catalogues directly
 * and the command handlers that query catalogues.
 *
 * Provides the event handlers that are concerned with the
 * state of catalogues and articles that are in catalogues.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class CatalogueWorkflow(private val eventBus: EventBus,
                        private val catalogues: Catalogues,
                        private val exceptionLogger: ExceptionLogger) : Workflow {
    //region Command Handlers
    /**
     * Creates a catalogue.
     * @code 201
     * @payload The created catalogue.
     * @publishes CatalogueCreatedEvent
     *
     * If the catalogue title is taken, logs the title.
     * @code 400
     *
     * If the catalogue requests a non-existing parent, logs the parent's id.
     * @code 400
     */
    @CommandHandler
    fun createCatalogue(command: CreateCatalogueCommand): CommandResponse {
        return try {
            val createdCatalogue = catalogues.create(command.catalogueRequest)

            eventBus.publish(CatalogueCreatedEvent(createdCatalogue))
            CommandResponse(StatusCode.Created, createdCatalogue)
        } catch (e: CatalogueTitleTakenException) {
            exceptionLogger.logException(e)
        } catch (e: CatalogueNotFoundException) {
            exceptionLogger.logException(e)
        }
    }

    /**
     * Deletes a catalogue.
     * @code 200
     * @payload The deleted catalogue.
     * @publishes CatalogueDeletedEvent
     *
     * If catalogue is not found by id, logs id.
     * @code 404
     */
    @CommandHandler
    fun deleteCatalogue(command: DeleteCatalogueCommand): CommandResponse {
        return try {
            val deletedCatalogue = catalogues.delete(command.catalogueId)

            eventBus.publish(CatalogueDeletedEvent(deletedCatalogue))
            CommandResponse(StatusCode.OK, deletedCatalogue)
        } catch (e: CatalogueNotFoundException) {
            exceptionLogger.logException(e)
        }
    }

    /**
     * Changes the title of a catalogue.
     * @code 200
     * @payload The updated catalogue.
     * @publishes CatalogueUpdatedEvent
     *
     * If the catalogue title is taken, logs title.
     * @code 400
     *
     * If the catalogue is not found by id, logs id.
     * @code 404
     */
    @CommandHandler
    fun changeCatalogueTitle(command: ChangeCatalogueTitleCommand): CommandResponse {
        return try {
            val updatedCatalogue = catalogues.changeTitle(command.catalogueId, command.newTitle)

            eventBus.publish(CatalogueUpdatedEvent(updatedCatalogue))
            CommandResponse(StatusCode.OK, updatedCatalogue)
        } catch (e: CatalogueTitleTakenException) {
            exceptionLogger.logException(e)
        } catch (e: CatalogueNotFoundException) {
            exceptionLogger.logException(e)
        }
    }

    /**
     * Changes the parent of a catalogue.
     * @code 200
     * @payload The updated catalogue.
     * @publishes CatalogueUpdatedEvent
     *
     * If the catalogue is already a child, logs the catalogue id and the parent title.
     * @code 400
     *
     * If the catalogue is not found by id, logs the id.
     * @code 404
     */
    @CommandHandler
    fun changeCatalogueParent(command: ChangeCatalogueParentCommand): CommandResponse {
        return try {
            val updatedCatalogue = catalogues.changeParentCatalogue(command.catalogueId, command.newParent)

            eventBus.publish(CatalogueUpdatedEvent(updatedCatalogue))
            CommandResponse(StatusCode.OK, updatedCatalogue)
        } catch (e: CatalogueAlreadyAChildException) {
            exceptionLogger.logException(e)
        } catch (e: CatalogueNotFoundException) {
            exceptionLogger.logException(e)
        }
    }

    /**
     * Appends a subcatalogue to a catalogue.
     * @code 200
     * @payload The updated catalogue.
     * @publishes CatalogueUpdatedEvent
     *
     * If the parent catalogue is also the child catalogue, logs the catalogue id.
     * @code 400
     *
     * If the child catalogue is already a child, logs the parent's and the child's ids.
     * @code 400
     *
     * If the parent catalogue is not found by id, logs the id.
     * @code 404
     */
    @CommandHandler
    fun appendSubCatalogue(command: AppendSubCatalogueCommand): CommandResponse {
        return try {
            val updatedCatalogue = catalogues.appendSubCatalogue(command.parentCatalogueId, command.childCatalogue)

            eventBus.publish(CatalogueUpdatedEvent(updatedCatalogue))
            CommandResponse(StatusCode.OK, updatedCatalogue)
        } catch (e: CatalogueAlreadyAChildException) {
            exceptionLogger.logException(e)
        } catch (e: SelfContainedCatalogueException) {
            exceptionLogger.logException(e)
        } catch (e: CatalogueNotFoundException) {
            exceptionLogger.logException(e)
        }
    }

    /**
     * Switches the order of two subcatalogues.
     * @code 200
     * @payload The updated catalogue.
     * @publishes CatalogueUpdatedEvent
     *
     * If the catalogue does not contain both subcatalogues, logs the catalogue's id and the subcatalogues' ids.
     * @code 400
     *
     * If the catalogue is not found by id, logs the id
     * @code 404
     */
    @CommandHandler
    fun switchSubCatalogues(command: SwitchSubCataloguesCommand): CommandResponse {
        return try {
            val updatedCatalogue = catalogues.switchSubCatalogues(
                    command.catalogueId, command.first, command.second)

            eventBus.publish(CatalogueUpdatedEvent(updatedCatalogue))
            CommandResponse(StatusCode.OK, updatedCatalogue)
        } catch (e: CatalogueNotAChildException) {
            exceptionLogger.logException(e)
        } catch (e: CatalogueNotFoundException) {
            exceptionLogger.logException(e)
        }
    }

    /**
     * Removes a subcatalogue from a parent catalogue.
     * @code 200
     * @payload The updated catalogue.
     * @publishes CatalogueUpdatedEvent
     *
     * If the subcatalogue is not a child, logs the parent's and the child's ids.
     * @code 400
     *
     * If the parent catalogue is not found by id, logs the id.
     * @code 404
     */
    @CommandHandler
    fun removeSubCatalogue(command: RemoveSubCatalogueCommand): CommandResponse {
        return try {
            val updatedCatalogue = catalogues.removeSubCatalogue(command.parentCatalogueId, command.childCatalogue)

            eventBus.publish(CatalogueUpdatedEvent(updatedCatalogue))
            CommandResponse(StatusCode.OK, updatedCatalogue)
        } catch (e: CatalogueNotAChildException) {
            exceptionLogger.logException(e)
        } catch (e: CatalogueNotFoundException) {
            exceptionLogger.logException(e)
        }
    }

    /**
     * Appends a article to a catalogue.
     * @code 200
     * @payload The updated catalogue.
     * @publishes CatalogueUpdatedEvent
     *
     * If the article is already a child, logs the article's and the catalogue's ids.
     * @code 400
     *
     * If the parent catalogue is not found by id, logs the id.
     * @code 404
     */
    @CommandHandler
    fun appendArticle(command: AppendArticleToCatalogueCommand): CommandResponse {
        return try {
            val updatedCatalogue = catalogues.appendArticle(command.parentCatalogueId, command.article)

            eventBus.publish(CatalogueUpdatedEvent(updatedCatalogue))
            CommandResponse(StatusCode.OK, updatedCatalogue)
        } catch (e: ArticleAlreadyInCatalogueException) {
            exceptionLogger.logException(e)
        } catch (e: CatalogueNotFoundException) {
            exceptionLogger.logException(e)
        }
    }

    /**
     * Switches the order of two articles in a catalogue.
     * @code 200
     * @payload The updated catalogue.
     * @publishes CatalogueUpdatedEvent
     *
     * If the catalogue does not contain both articles, logs the catalogue's id and the articles' ids.
     * @code 400
     *
     * If the catalogue is not found by id, logs the id
     * @code 404
     */
    @CommandHandler
    fun switchArticlesInCatalogue(command: SwitchArticlesInCatalogueCommand): CommandResponse {
        return try {
            val updatedCatalogue = catalogues.switchArticles(
                    command.catalogueId, command.first, command.second)

            eventBus.publish(CatalogueUpdatedEvent(updatedCatalogue))
            CommandResponse(StatusCode.OK, updatedCatalogue)
        } catch (e: ArticleNotInCatalogueException) {
            exceptionLogger.logException(e)
        } catch (e: CatalogueNotFoundException) {
            exceptionLogger.logException(e)
        }
    }

    /**
     * Removes an article from a catalogue.
     * @code 200
     * @payload The updated catalogue.
     * @publishes CatalogueUpdatedEvent
     *
     * If the article is not contained in the catalogue, logs the article's and the catalogue's ids.
     * @code 400
     *
     * If the parent catalogue is not found by id, logs the id.
     * @code 404
     */
    @CommandHandler
    fun removeArticle(command: RemoveArticleFromCatalogueCommand): CommandResponse {
        return try {
            val updatedCatalogue = catalogues.removeArticle(command.parentCatalogueId, command.article)

            eventBus.publish(CatalogueUpdatedEvent(updatedCatalogue))
            CommandResponse(StatusCode.OK, updatedCatalogue)
        } catch (e: ArticleNotInCatalogueException) {
            exceptionLogger.logException(e)
        } catch (e: CatalogueNotFoundException) {
            exceptionLogger.logException(e)
        }
    }

    /**
     * Retrieves a catalogue by id.
     * @code 200
     * @payload The retrieved catalogue.
     *
     * If no catalogue is found, logs id.
     * @code 404
     */
    @CommandHandler
    fun getCatalogueById(command: GetCatalogueByIdCommand): CommandResponse {
        val possibleCatalogue = catalogues.getById(command.catalogueId)

        return if (possibleCatalogue.isPresent) CommandResponse(StatusCode.OK, possibleCatalogue.get())
        else exceptionLogger.logException(CatalogueNotFoundException(command.catalogueId))
    }

    //endregion

    //region Event Handlers


    //endregion
}

//region Queries

data class GetCatalogueByIdCommand(val catalogueId: String) : Command

//endregion

//region Commands

data class CreateCatalogueCommand(val catalogueRequest: CatalogueRequest) : Command
data class CatalogueCreatedEvent(val catalogue: Catalogue) : Event

data class DeleteCatalogueCommand(val catalogueId: String) : Command
data class CatalogueDeletedEvent(val catalogue: Catalogue) : Event

data class ChangeCatalogueTitleCommand(val catalogueId: String, val newTitle: String) : Command
data class ChangeCatalogueParentCommand(val catalogueId: String, val newParent: Catalogue) : Command
data class AppendSubCatalogueCommand(val parentCatalogueId: String, val childCatalogue: Catalogue) : Command
data class RemoveSubCatalogueCommand(val parentCatalogueId: String, val childCatalogue: Catalogue) : Command
data class AppendArticleToCatalogueCommand(val parentCatalogueId: String, val article: Article) : Command
data class RemoveArticleFromCatalogueCommand(val parentCatalogueId: String, val article: Article) : Command
data class SwitchArticlesInCatalogueCommand(val catalogueId: String, val first: Article, val second: Article) : Command
data class SwitchSubCataloguesCommand(val catalogueId: String, val first: Catalogue, val second: Catalogue) : Command
data class CatalogueUpdatedEvent(val catalogue: Catalogue) : Event

//endregion