package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleNotFoundException
import com.tsbonev.nharker.core.ArticleProperties
import com.tsbonev.nharker.core.ArticleRequest
import com.tsbonev.nharker.core.ArticleTitleTakenException
import com.tsbonev.nharker.core.Articles
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryAlreadyInArticleException
import com.tsbonev.nharker.core.EntryNotInArticleException
import com.tsbonev.nharker.core.OrderedReferenceMap
import com.tsbonev.nharker.core.PropertyNotFoundException
import com.tsbonev.nharker.cqrs.Command
import com.tsbonev.nharker.cqrs.CommandHandler
import com.tsbonev.nharker.cqrs.CommandResponse
import com.tsbonev.nharker.cqrs.Event
import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.cqrs.EventHandler
import com.tsbonev.nharker.cqrs.StatusCode
import com.tsbonev.nharker.cqrs.Workflow
import com.tsbonev.nharker.cqrs.isSuccess
import com.tsbonev.nharker.server.helpers.ExceptionLogger

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
                      private val articles: Articles,
                      private val exceptionLogger: ExceptionLogger) : Workflow {
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
            exceptionLogger.logException(e)
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
                        eventBus.send(DeleteEntryCommand(it))
                    }

            deletedArticle.entries.raw()
                    .keys
                    .forEach {
                        eventBus.send(DeleteEntryCommand(it))
                    }

            eventBus.publish(ArticleDeletedEvent(deletedArticle))

            CommandResponse(StatusCode.OK, deletedArticle)
        } catch (e: ArticleNotFoundException) {
            exceptionLogger.logException(e)
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
            exceptionLogger.logException(ArticleNotFoundException(command.articleId))
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
            exceptionLogger.logException(e)
        } catch (e: EntryAlreadyInArticleException) {
            exceptionLogger.logException(e)
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
            exceptionLogger.logException(e)
        } catch (e: EntryNotInArticleException) {
            exceptionLogger.logException(e)
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
            exceptionLogger.logException(e)
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
            exceptionLogger.logException(e)
        } catch (e: PropertyNotFoundException) {
            exceptionLogger.logException(e)
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
        } catch (e: ArticleNotFoundException) {
            exceptionLogger.logException(e)
        } catch (e: EntryNotInArticleException) {
            exceptionLogger.logException(e)
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
            exceptionLogger.logException(ArticleNotFoundException(command.linkTitle))
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
    /**
     * Saves a restored article and restores its linked entries.
     * @spawns EntryRestoredEvent
     */
    @EventHandler
    fun onArticleRestored(event: EntityRestoredEvent) {
        if (event.entityClass == Article::class.java && event.entity is Article) {

            val restoredArticle = event.entity

            val rebuiltArticle = restoredArticle
                    .rebuild()
                    .restoreEntries(restoredArticle.entries)
                    .restoreProperties(restoredArticle.properties)
                    .restoreLinks()

            articles.save(rebuiltArticle)
        }
    }
    //endregion

    /**
     * Sets up an Article for rebuilding by recreating the
     * article with the same id, titles and creation date but
     * leaving out the entries and properties.
     */
    private fun Article.rebuild(): Article {
        return Article(
                this.id,
                this.linkTitle,
                this.fullTitle,
                this.creationDate,
                this.catalogues
        )
    }

    /**
     * Restores the entries of an article by either
     * assuring that they exist or by restoring them from
     * the trash. If an entry does not exist and is not found in the trash
     * it is skipped.
     *
     * @param oldEntries The entries to restore.
     * @return The article with restored entries.
     */
    private fun Article.restoreEntries(oldEntries: OrderedReferenceMap): Article {
        val newEntries = mutableMapOf<String, Int>()

        oldEntries.raw()
                .forEach { entryId, _ ->
                    val getResponse = eventBus.send(GetEntryByIdCommand(entryId))
                    if (getResponse.statusCode.isSuccess()) {
                        val fetchedEntry = getResponse.payload.get() as Entry
                        newEntries[fetchedEntry.id] = newEntries.count()
                    } else {
                        val restoreResponse = eventBus.send(
                                RestoreTrashedEntityCommand(entryId, Entry::class.java))
                        if (restoreResponse.statusCode.isSuccess()) {
                            val restoredEntry = restoreResponse.payload.get() as Entry
                            newEntries[restoredEntry.id] = newEntries.count()
                        }
                    }
                }
        return this.copy(entries = OrderedReferenceMap(newEntries as LinkedHashMap<String, Int>))
    }

    /**
     * Restores the properties of an entry by either
     * fetching them by id and attaching them or restoring them
     * from the trash store. If an entry is not found in persistence or
     * the trash it is skipped.
     *
     * @param oldProperties The properties to restore.
     * @return The article with restored properties.
     */
    private fun Article.restoreProperties(oldProperties: ArticleProperties): Article {
        oldProperties.getAll()
                .forEach { propName, entryId ->
                    val getResponse = eventBus.send(GetEntryByIdCommand(entryId))
                    if (getResponse.statusCode.isSuccess()) {
                        val restoredEntry = getResponse.payload.get() as Entry
                        this.properties.attachProperty(propName, restoredEntry.id)
                    } else {
                        val restoreResponse = eventBus.send(
                                RestoreTrashedEntityCommand(entryId, Entry::class.java))
                        if (restoreResponse.statusCode.isSuccess()) {
                            val restoredEntry = restoreResponse.payload.get() as Entry
                            this.properties.attachProperty(propName, restoredEntry.id)
                        }
                    }
                }
        return this
    }

    /**
     * Restores the links of an article by going through its
     * entries and properties and adding their links to its own.
     *
     * @return The article with restored links.
     */
    private fun Article.restoreLinks(): Article {
        this.properties.getAll().forEach { link, entryId ->
            val getResponse = eventBus.send(GetEntryByIdCommand(entryId))
            if (getResponse.statusCode.isSuccess()) {
                this.links.addLink(link)
            } else {
                val restoreResponse = eventBus.send(
                        RestoreTrashedEntityCommand(entryId, Entry::class.java))
                if (restoreResponse.statusCode.isSuccess()) {
                    this.links.addLink(link)
                }
            }
        }

        this.entries.raw().forEach { entryId, _ ->
            val response = eventBus.send(
                    GetEntryByIdCommand(entryId))
            if (response.statusCode.isSuccess()) {
                val restoredEntry = response.payload.get() as Entry
                restoredEntry.links.forEach { _, link ->
                    this.links.addLink(link)
                }
            }
        }

        return this
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