package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleSynonymProvider
import com.tsbonev.nharker.core.SynonymAlreadyTakenException
import com.tsbonev.nharker.core.SynonymNotFoundException
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
 * Provides the command handlers that are concerned with the
 * direct state of the global synonym map.
 *
 * Provides the event handlers that update the global synonym
 * map.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class ArticleSynonymWorkflow(
	private val eventBus: EventBus,
	private val synonyms: ArticleSynonymProvider,
	private val exceptionLogger: ExceptionLogger
) : Workflow {
	//region Command Handlers
	/**
	 * Adds a synonym to the global map.
	 * @code 201
	 * @payload A pair of the added synonym and its article.
	 * @publishes SynonymAddedEvent
	 *
	 * If the synonym is already taken, logs the name of the synonym.
	 * @code 400
	 * @exception SynonymAlreadyTakenException
	 */
	fun addSynonym(command: AddSynonymCommand): CommandResponse {
		return try {
			val addedSynonym = synonyms.addSynonym(command.synonym, command.article)
			eventBus.publish(SynonymAddedEvent(addedSynonym, command.article))

			CommandResponse(StatusCode.Created, Pair(addedSynonym, command.article))
		} catch (e: SynonymAlreadyTakenException) {
			exceptionLogger.logException(e)
		}
	}

	/**
	 * Removes a synonym from the global synonym map.
	 * @code 200
	 * @payload The removed synonym.
	 * @publishes
	 *
	 * If the synonym is not found, logs the synonym name.
	 * @code 404
	 * @exception SynonymNotFoundException
	 */
	fun removeSynonym(command: RemoveSynonymCommand): CommandResponse {
		return try {
			val removedSynonymIdPair = synonyms.removeSynonym(command.synonym)
			eventBus.publish(SynonymRemovedEvent(removedSynonymIdPair.first, removedSynonymIdPair.second))

			CommandResponse(StatusCode.OK, removedSynonymIdPair)
		} catch (e: SynonymNotFoundException) {
			exceptionLogger.logException(e)
		}
	}

	/**
	 * Returns the whole global synonym map.
	 * @code 200
	 * @payload A map of synonyms and article link titles.
	 */
	@Suppress("UNUSED_PARAMETER")
	@CommandHandler
	fun getSynonymMap(query: GetSynonymMapQuery): QueryResponse {
		return QueryResponse(StatusCode.OK, synonyms.getSynonymMap())
	}

	/**
	 * Returns a list of synonyms associated with an article.
	 * @code 200
	 * @payload A list of synonyms.
	 */
	@CommandHandler
	fun getSynonymsForArticle(query: GetSynonymsForArticleQuery): QueryResponse {
		val synonymList = mutableListOf<String>()

		synonyms.getSynonymMap()
			.filter { it.value == query.article.id }
			.mapTo(synonymList) { it.key }

		return QueryResponse(StatusCode.OK, synonymList)
	}

	/**
	 * Searches for a synonym in the global synonym map.
	 * @code 200
	 * @payload The found article link title to which the synonym points.
	 *
	 * If no synonym matches the search string, returns not found.
	 * @code 404
	 */
	@CommandHandler
	fun searchSynonymMap(query: SearchSynonymMapQuery): QueryResponse {
		val foundLink = synonyms.getSynonymMap()[query.searchString]

		return if (foundLink != null) {
			QueryResponse(StatusCode.OK, foundLink)
		} else {
			QueryResponse(StatusCode.NotFound)
		}
	}
	//endregion

	//region Event Handlers
	/**
	 * Removes the synonyms of a deleted article.
	 * @publishes SynonymRemovedEvent
	 */
	@EventHandler
	fun onArticleDeletedRemoveSynonyms(event: ArticleDeletedEvent) {
		synonyms.getSynonymMap()
			.filter { it.value == event.article.id }
			.forEach { key, id ->
				synonyms.removeSynonym(key)
				eventBus.publish(SynonymRemovedEvent(key, id))
			}
	}
	//endregion
}

//region Queries
class GetSynonymMapQuery : Query

data class GetSynonymsForArticleQuery(val article: Article) : Query
data class SearchSynonymMapQuery(val searchString: String) : Query
//endregion

//region Commands
data class AddSynonymCommand(val synonym: String, val article: Article) : Command

data class SynonymAddedEvent(val synonym: String, val article: Article) : Event

data class RemoveSynonymCommand(val synonym: String) : Command
data class SynonymRemovedEvent(val synonym: String, val articleId: String) : Event
//endregion