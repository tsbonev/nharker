package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleNotFoundException
import com.tsbonev.nharker.core.ArticleRequest
import com.tsbonev.nharker.core.ArticleTitleTakenException
import com.tsbonev.nharker.core.Articles
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryAlreadyInArticleException
import com.tsbonev.nharker.core.EntryNotInArticleException
import com.tsbonev.nharker.core.PropertyNotFoundException
import com.tsbonev.nharker.cqrs.Command
import com.tsbonev.nharker.cqrs.CommandHandler
import com.tsbonev.nharker.cqrs.CommandResponse
import com.tsbonev.nharker.cqrs.Event
import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.cqrs.Workflow
import com.tsbonev.nharker.cqrs.StatusCode
import org.slf4j.LoggerFactory

/**
 * Provides the command handlers that affect articles directly
 * and the command handlers that query articles.
 *
 * Provides the event handlers that are concerned with the
 * state of articles and entries that are in articles.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class ArticleWorkflow(private val eventBus: EventBus,
                      private val articles: Articles) : Workflow {
    private val logger = LoggerFactory.getLogger("ArticleWorkflow")

    //region Command Handlers

    /**
     * Creates an article.
     * @code 201
     * @payload The created article.
     * @publishes ArticleCreatedEvent
     *
     * If the article title is taken, logs the title.
     * @code 400
     */
    @CommandHandler
    fun createArticle(command: CreateArticleCommand): CommandResponse {
        return try {
            val createdArticle = articles.create(command.articleRequest)
            eventBus.publish(ArticleCreatedEvent(createdArticle))

            CommandResponse(StatusCode.Created, createdArticle)
        } catch (e: ArticleTitleTakenException) {
            logger.error("There is already an article with the title ${command.articleRequest.fullTitle}!")
            CommandResponse(StatusCode.BadRequest)
        }
    }

    /**
     * Deletes and article and its entries and properties.
     * @code 200
     * @payload The deleted article.
     * @publishes ArticleDeletedEvent
     * @spawns DeleteEntryCommand
     *
     * If the article is not found, logs the id.
     * @code 404
     */
    @CommandHandler
    fun deleteArticle(command: DeleteArticleCommand): CommandResponse {
        return try {
            val deletedArticle = articles.delete(command.articleId)

            deletedArticle.properties
                    .getAll()
                    .values
                    .forEach {
                        eventBus.send(DeleteEntryCommand(it.id))
                    }

            deletedArticle.entries
                    .keys
                    .forEach {
                        eventBus.send(DeleteEntryCommand(it))
                    }

            eventBus.publish(ArticleDeletedEvent(deletedArticle))

            CommandResponse(StatusCode.OK, deletedArticle)
        } catch (e: ArticleNotFoundException) {
            logArticleNotFound(command.articleId)
        }
    }


    /**
     * Retrieves an article by id.
     * @code 200
     * @payload The retrieve article.
     *
     * If an article is not found, logs the id.
     * @code 404
     */
    @CommandHandler
    fun getArticleById(command: GetArticleByIdCommand): CommandResponse {
        val possibleArticle = articles.getById(command.articleId)

        return if (possibleArticle.isPresent) CommandResponse(StatusCode.OK, possibleArticle.get())
        else {
            logArticleNotFound(command.articleId)
        }
    }

    /**
     * Appends an entry to an article.
     * @code 200
     * @payload The updated article.
     * @publishes ArticleUpdatedEvent
     *
     * If an article is not found by id, logs id.
     * @code 404
     *
     * If the entry is already in the article, logs entry id.
     * @code 400
     */
    @CommandHandler
    fun appendEntryToArticle(command: AppendEntryToArticleCommand): CommandResponse {
        return try {
            val updatedArticle = articles.appendEntry(command.articleId, command.entry)

            eventBus.publish(ArticleUpdatedEvent(updatedArticle))
            CommandResponse(StatusCode.OK, updatedArticle)
        } catch (e: ArticleNotFoundException) {
            logArticleNotFound(command.articleId)
        } catch (e: EntryAlreadyInArticleException) {
            logger.error("The article with id ${command.articleId} already contains the entry with id ${command.entry.id}!")
            CommandResponse(StatusCode.BadRequest)
        }
    }

    /**
     * Removes an entry from an article.
     * @code 200
     * @payload The updated article.
     * @publishes ArticleUpdatedEvent
     *
     * If an article is not found by id, logs id.
     * @code 404
     *
     * If the entry is not in the article, logs entry id.
     * @code 400
     */
    @CommandHandler
    fun removeEntryFromArticle(command: RemoveEntryFromArticleCommand): CommandResponse {
        return try {
            val updatedArticle = articles.removeEntry(command.articleId, command.entry)

            eventBus.send(DeleteEntryCommand(command.entry.id))

            eventBus.publish(ArticleUpdatedEvent(updatedArticle))
            CommandResponse(StatusCode.OK, updatedArticle)
        } catch (e: ArticleNotFoundException) {
            logArticleNotFound(command.articleId)
        } catch (e: EntryNotInArticleException) {
            logger.error("The article with id ${command.articleId} does not contain an entry with id ${command.entry.id}!")
            CommandResponse(StatusCode.BadRequest)
        }
    }

    /**
     * Attaches a property to an article.
     * @code 200
     * @payload The updated article.
     * @publishes ArticleUpdatedEvent
     *
     * If the article is not found by id, logs id.
     * @code 404
     */
    @CommandHandler
    fun attachPropertyToArticle(command: AttachPropertyToArticleCommand): CommandResponse {
        return try {
            val updatedArticle = articles.attachProperty(
                    command.articleId,
                    command.propertyName,
                    command.property
            )

            eventBus.publish(ArticleUpdatedEvent(updatedArticle))
            CommandResponse(StatusCode.OK, updatedArticle)
        } catch (e: ArticleNotFoundException) {
            logArticleNotFound(command.articleId)
        }
    }

    /**
     * Detaches a property from an article.
     * @code 200
     * @payload The updated article.
     * @publishes ArticleUpdatedEvent
     *
     * If the article is not found by id, logs id.
     * @code 404
     *
     * If the article does not contain the property, logs property name and article id.
     * @code 400
     */
    @CommandHandler
    fun detachPropertyFromArticle(command: DetachPropertyFromArticleCommand): CommandResponse {
        return try {
            val updatedArticle = articles.detachProperty(
                    command.articleId,
                    command.propertyName
            )

            eventBus.publish(ArticleUpdatedEvent(updatedArticle))
            CommandResponse(StatusCode.OK, updatedArticle)
        } catch (e: ArticleNotFoundException) {
            logArticleNotFound(command.articleId)
        } catch (e: PropertyNotFoundException) {
            logger.error("Could not find property named ${command.propertyName} in article with id ${command.articleId}!")
            CommandResponse(StatusCode.BadRequest)
        }
    }

    /**
     * Switches two entries' order in an article.
     * @code 200
     * @payload The updated article.
     * @publishes ArticleUpdatedEvent
     *
     * If the article is not found by id, logs id.
     * @code 404
     *
     * If the entries are not both in the article, logs the entries' ids and the article's id.
     * @code 400
     */
    @CommandHandler
    fun switchEntriesInArticle(command: SwitchEntriesInArticleCommand): CommandResponse {
        return try {
            val updatedArticle = articles.switchEntries(command.articleId, command.first, command.second)

            eventBus.publish(ArticleUpdatedEvent(updatedArticle))
            CommandResponse(StatusCode.OK, updatedArticle)
        } catch (ex: ArticleNotFoundException) {
            logArticleNotFound(command.articleId)
        } catch (ex: EntryNotInArticleException) {
            logger.error("Entries with ids ${command.first.id} and ${command.second.id} are not in article with id ${command.articleId}")
            CommandResponse(StatusCode.BadRequest)
        }
    }

    /**
     * Retrieves an article by link title.
     * @code 200
     * @payload The retrieve article.
     *
     * If an article is not found, logs the link title.
     * @code 404
     */
    @CommandHandler
    fun getArticleByLinkTitle(command: GetArticleByLinkTitleCommand): CommandResponse {
        val possibleArticle = articles.getByLinkTitle(command.linkTitle)

        return if (possibleArticle.isPresent) CommandResponse(StatusCode.OK, possibleArticle.get())
        else {
            logger.error("Could not find article with link title ${command.linkTitle}!")
            CommandResponse(StatusCode.NotFound)
        }
    }

    /**
     * Searches for an article by matching its full title.
     * @code 200
     * @payload A list of matched articles.
     */
    @CommandHandler
    fun searchArticlesByTitle(command: SearchArticleByTitleCommand): CommandResponse {
        return CommandResponse(StatusCode.OK, articles.searchByFullTitle(command.searchString))
    }

    /**
     * Returns a list of full titles retrieved by link titles.
     * @code 200
     * @payload A list of article full titles.
     */
    @CommandHandler
    fun retrieveFullTitles(command: RetrieveFullTitlesCommand): CommandResponse {
        return CommandResponse(StatusCode.OK, articles.getArticleTitles(command.linkTitles))
    }

    //endregion

    //region Event Handlers

    //endregion

    private fun logArticleNotFound(id: String): CommandResponse {
        logger.error("Could not find article with id $id!")
        return CommandResponse(StatusCode.NotFound)
    }
}

//region Queries

data class GetArticleByIdCommand(val articleId: String) : Command
data class GetArticleByLinkTitleCommand(val linkTitle: String) : Command
data class SearchArticleByTitleCommand(val searchString: String) : Command
data class RetrieveFullTitlesCommand(val linkTitles: Set<String>) : Command

//endregion

//region Commands

data class CreateArticleCommand(val articleRequest: ArticleRequest) : Command
data class ArticleCreatedEvent(val article: Article) : Event

data class DeleteArticleCommand(val articleId: String) : Command
data class ArticleDeletedEvent(val article: Article) : Event

data class AppendEntryToArticleCommand(val entry: Entry, val articleId: String) : Command
data class RemoveEntryFromArticleCommand(val entry: Entry, val articleId: String) : Command
data class AttachPropertyToArticleCommand(val propertyName: String, val property: Entry, val articleId: String) : Command
data class DetachPropertyFromArticleCommand(val propertyName: String, val articleId: String) : Command
data class SwitchEntriesInArticleCommand(val articleId: String, val first: Entry, val second: Entry) : Command
data class ArticleUpdatedEvent(val article: Article) : Event

//endregion