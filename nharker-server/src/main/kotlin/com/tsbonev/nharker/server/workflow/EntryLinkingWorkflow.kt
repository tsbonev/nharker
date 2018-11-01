package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryLinker
import com.tsbonev.nharker.cqrs.Command
import com.tsbonev.nharker.cqrs.CommandHandler
import com.tsbonev.nharker.cqrs.CommandResponse
import com.tsbonev.nharker.cqrs.Event
import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.cqrs.StatusCode
import com.tsbonev.nharker.cqrs.Workflow

/**
 * Provides the commands to affect the links between entries and articles.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class EntryLinkingWorkflow(
	private val eventBus: EventBus,
	private val linker: EntryLinker
) : Workflow {
	//region Command Handlers
	/**
	 * Links an entry's content to matching article's titles or
	 * to synonyms from the global synonym map.
	 * @code 200
	 * @payload The linked entry.
	 * @publishes EntryLinkedEvent
	 */
	@CommandHandler
	fun linkEntry(command: LinkEntryContentToArticlesCommand): CommandResponse{
		val linkedEntry = linker.linkEntryToArticles(command.entry)

		eventBus.publish(EntryLinkedEvent(linkedEntry))
		return CommandResponse(StatusCode.OK, linkedEntry)
	}

	/**
	 * Refreshes an article's entries' links.
	 * @code 200
	 * @payload The refreshed entries.
	 * @publishes ArticleLinksRefreshedEvent
	 */
	@CommandHandler
	fun refreshArticleLinks(command: RefreshArticleEntryLinksCommand): CommandResponse{
		val refreshedEntries = linker.refreshLinksOfArticle(command.article)

		eventBus.publish(ArticleLinksRefreshedEvent(command.article, refreshedEntries))
		return CommandResponse(StatusCode.OK, refreshedEntries)
	}
	//endregion

	//region Event Handlers

	//endregion
}
//region Commands
data class RefreshArticleEntryLinksCommand(val article: Article): Command
data class ArticleLinksRefreshedEvent(val article: Article, val entries: List<Entry>): Event

data class LinkEntryContentToArticlesCommand(val entry: Entry): Command
data class EntryLinkedEvent(val entry: Entry): Event
//endregion