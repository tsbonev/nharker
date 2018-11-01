package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleNotFoundException
import com.tsbonev.nharker.core.ArticlePaginationException
import com.tsbonev.nharker.core.ArticleProperties
import com.tsbonev.nharker.core.ArticleRequest
import com.tsbonev.nharker.core.ArticleTitleTakenException
import com.tsbonev.nharker.core.Articles
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryAlreadyInArticleException
import com.tsbonev.nharker.core.EntryNotInArticleException
import com.tsbonev.nharker.core.OrderedReferenceMap
import com.tsbonev.nharker.core.PropertyNotFoundException
import com.tsbonev.nharker.core.SortBy
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
class ArticleWorkflow(
	private val eventBus: EventBus,
	private val articles: Articles,
	private val exceptionLogger: ExceptionLogger
) : Workflow {
	//region Command Handlers
	/**
	 * Creates an article.
	 * @code 201
	 * @payload The created article.
	 * @publishes ArticleCreatedEvent
	 *
	 * If the article title is taken, logs the title.
	 * @code 400
	 * @exception ArticleTitleTakenException
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
	 * @exception ArticleNotFoundException
	 */
	@CommandHandler
	fun deleteArticle(command: DeleteArticleCommand): CommandResponse {
		return try {
			val deletedArticle = articles.deleteById(command.articleId)

			deletedArticle.properties
				.raw()
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
	 * Appends an entry to an article.
	 * @code 200
	 * @payload The updated article.
	 * @publishes ArticleUpdatedEvent
	 *
	 * If an article is not found by id, logs id.
	 * @code 404
	 * @exception ArticleNotFoundException
	 *
	 * If the entry is already in the article, logs entry id.
	 * @code 400
	 * @exception EntryAlreadyInArticleException
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
	 * @spawns DeleteEntryCommand
	 *
	 * If an article is not found by id, logs id.
	 * @code 404
	 * @exception ArticleNotFoundException
	 *
	 * If the entry is not in the article, logs entry id.
	 * @code 400
	 * @exception EntryNotInArticleException
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
	 * @exception ArticleNotFoundException
	 */
	@CommandHandler
	fun attachPropertyToArticle(command: AttachPropertyToArticleCommand): CommandResponse {
		return try {
			val updatedArticle = articles.attachProperty(
				command.articleId,
				command.propName,
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
	 * @spawns DeleteEntryCommand
	 *
	 * If the article is not found by id, logs id.
	 * @code 404
	 * @exception ArticleNotFoundException
	 *
	 * If the article does not contain the property, logs property name and article id.
	 * @code 400
	 * @exception PropertyNotFoundException
	 */
	@CommandHandler
	fun detachPropertyFromArticle(command: DetachPropertyFromArticleCommand): CommandResponse {
		return try {
			val updatedArticleEntryIdPair = articles.detachProperty(
				command.articleId,
				command.propName
			)

			eventBus.send(DeleteEntryCommand(updatedArticleEntryIdPair.second))

			eventBus.publish(ArticleUpdatedEvent(updatedArticleEntryIdPair.first))
			CommandResponse(StatusCode.OK, updatedArticleEntryIdPair)
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
	 * @exception ArticleNotFoundException
	 *
	 * If the entries are not both in the article, logs the entries' ids and the article's id.
	 * @code 400
	 * @exception EntryNotInArticleException
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
	 * Retrieves an article by id.
	 * @code 200
	 * @payload The retrieve article.
	 *
	 * If an article is not found, logs the id.
	 * @code 404
	 * @exception ArticleNotFoundException
	 */
	@CommandHandler
	fun getArticleById(command: GetArticleByIdQuery): QueryResponse {
		val possibleArticle = articles.getById(command.articleId)

		return if (possibleArticle.isPresent) QueryResponse(StatusCode.OK, possibleArticle.get())
		else exceptionLogger.logException(ArticleNotFoundException(command.articleId))
	}

	/**
	 * Searches for an article by matching its title.
	 * @code 200
	 * @payload A list of matched articles.
	 */
	@CommandHandler
	fun searchArticlesByTitle(query: SearchArticleByTitleQuery): QueryResponse {
		return QueryResponse(StatusCode.OK, articles.searchByFullTitle(query.searchString))
	}

	/**
	 * Retrieves all articles.
	 * @code 200
	 * @payload A list of articles.
	 */
	fun getAllArticles(query: GetAllArticlesQuery): QueryResponse {
		val articleList = articles.getAll(query.order)
		return CommandResponse(StatusCode.OK, articleList)
	}

	/**
	 * Retrieves articles and paginates them.
	 * @code 200
	 * @payload A list of articles.
	 *
	 * If an illegal page size or page index are passed, logs an error with the size and index.
	 * @code 400
	 * @exception ArticlePaginationException
	 */
	@CommandHandler
	fun getPaginatedArticles(query: GetPaginatedArticlesQuery): QueryResponse {
		return try {
			val articleList = articles.getPaginated(query.order, query.page, query.pageSize)
			CommandResponse(StatusCode.OK, articleList)
		} catch (e: ArticlePaginationException) {
			exceptionLogger.logException(e)
		}
	}
	//endregion

	//region Event Handlers
	/**
	 * Saves a restored article and restores all of its entries.
	 * @spawns RestoreTrashedEntityCommand
	 */
	@EventHandler
	fun onArticleRestored(event: EntityRestoredEvent) {
		if (event.entityClass == Article::class.java && event.entity is Article) {
			val restoredArticle = event.entity

			val restoredEntries = mutableListOf<Entry>()
			val restoredProperties = mutableListOf<Entry>()

			val rebuiltArticle = restoredArticle
				.rebuild()
				.restoreEntries(restoredArticle.entries, restoredEntries)
				.restoreProperties(restoredArticle.properties, restoredProperties)

			articles.save(rebuiltArticle)
		}
	}

	/**
	 * Saves an article that has had its entries' links refreshed.
	 * @spawns LinkEntryContentToArticlesCommand
	 */
	@EventHandler
	fun onArticleRefreshed(event: ArticleLinksRefreshedEvent) {
		event.entries.forEach {
			eventBus.send(LinkEntryContentToArticlesCommand(it))
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
			this.title,
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
	 * @param restoredEntries The list to keep the restored entries in.
	 * @return The article with restored entries.
	 */
	private fun Article.restoreEntries(
		oldEntries: OrderedReferenceMap,
		restoredEntries: MutableList<Entry>
	): Article {
		val entryIds = oldEntries.raw().keys

		restoreEntriesFromIds(entryIds).mapTo(restoredEntries) { it -> it }

		restoredEntries.forEach {
			this.entries.append(it.id)
		}

		return this
	}

	/**
	 * Restores the properties of an entry by either
	 * fetching them by id and attaching them or restoring them
	 * from the trash store. If an entry is not found in persistence or
	 * the trash it is skipped.
	 *
	 * @param oldProperties The properties to restore.
	 * @param restoredProperties The list to keep the restored properties in.
	 * @return The article with restored properties.
	 */
	private fun Article.restoreProperties(
		oldProperties: ArticleProperties,
		restoredProperties: MutableList<Entry>
	): Article {
		val entryIds = oldProperties.raw().values

		restoreEntriesFromIds(entryIds).mapTo(restoredProperties) { it -> it }

		oldProperties.raw().forEach { propertyName, propertyReference ->
			if (restoredProperties.filter { it.id == propertyReference }.any()) {
				this.properties.attachProperty(propertyName, propertyReference)
			}
		}

		return this
	}

	/**
	 * Restores a list of entries by a given list of ids.
	 *
	 * @spawns RestoreTrashedEntityCommand
	 * @param refList The list of ids to restore.
	 * @return A list of restored entries.
	 */
	private fun restoreEntriesFromIds(refList: Collection<String>): List<Entry> {
		val entryList = mutableListOf<Entry>()

		refList.forEach {
			val getResponse = eventBus.send(GetEntryByIdQuery(it))
			if (getResponse.statusCode.isSuccess()) {
				val fetchedEntry = getResponse.payload.get() as Entry
				entryList.add(fetchedEntry)
			} else {
				val restoreResponse = eventBus.send(
					RestoreTrashedEntityCommand(it, Entry::class.java)
				)
				if (restoreResponse.statusCode.isSuccess()) {
					val restoredEntry = restoreResponse.payload.get() as Entry
					entryList.add(restoredEntry)
				}
			}
		}

		return entryList
	}
}

//region Queries
data class GetArticleByIdQuery(val articleId: String) : Query

data class SearchArticleByTitleQuery(val searchString: String) : Query

data class GetAllArticlesQuery(val order: SortBy) : Query

data class GetPaginatedArticlesQuery(val order: SortBy, val page: Int, val pageSize: Int) : Query
//endregion

//region Commands
data class CreateArticleCommand(val articleRequest: ArticleRequest) : Command

data class ArticleCreatedEvent(val article: Article) : Event

data class DeleteArticleCommand(val articleId: String) : Command
data class ArticleDeletedEvent(val article: Article) : Event

data class AppendEntryToArticleCommand(val entry: Entry, val articleId: String) : Command
data class RemoveEntryFromArticleCommand(val entry: Entry, val articleId: String) : Command
data class AttachPropertyToArticleCommand(val propName: String, val property: Entry, val articleId: String) : Command
data class DetachPropertyFromArticleCommand(val propName: String, val articleId: String) : Command
data class SwitchEntriesInArticleCommand(val articleId: String, val first: Entry, val second: Entry) : Command
data class ArticleUpdatedEvent(val article: Article) : Event
//endregion