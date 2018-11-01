@file:Suppress("UNCHECKED_CAST")

package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryLinker
import com.tsbonev.nharker.core.OrderedReferenceMap
import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.cqrs.StatusCode
import org.jmock.AbstractExpectations.returnValue
import org.jmock.Expectations
import org.jmock.Mockery
import org.jmock.integration.junit4.JUnitRuleMockery
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class EntryLinkerWorkflowTest {
	@Rule
	@JvmField
	val context: JUnitRuleMockery = JUnitRuleMockery()

	private val date = LocalDateTime.ofEpochSecond(1, 1, ZoneOffset.UTC)

	private val entry = Entry(
		"::entry-id::",
		date,
		"::article-id::",
		"::content::"
	)

	private val article = Article(
		"::article-id::",
		"Article title",
		date,
		entries = OrderedReferenceMap(linkedMapOf(entry.id to 0))
	)

	private val eventBus = context.mock(EventBus::class.java)
	private val linker = context.mock(EntryLinker::class.java)

	private val linkingWorkflow = EntryLinkingWorkflow(eventBus, linker)

	@Test
	fun `Links entry to articles`(){
		context.expecting {
			oneOf(linker).linkEntryToArticles(entry)
			will(returnValue(entry))

			oneOf(eventBus).publish(EntryLinkedEvent(entry))
		}

		val response = linkingWorkflow.linkEntry(
			LinkEntryContentToArticlesCommand(entry)
		)

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as Entry, Is(entry))
	}

	@Test
	fun `Refreshes articles entry links`(){
		context.expecting {
			oneOf(linker).refreshLinksOfArticle(article)
			will(returnValue(listOf(entry)))

			oneOf(eventBus).publish(ArticleLinksRefreshedEvent(article, listOf(entry)))
		}

		val response = linkingWorkflow.refreshArticleLinks(
			RefreshArticleEntryLinksCommand(article)
		)

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as List<Entry>, Is(listOf(entry)))
	}

	@Test
	fun `Refreshes links of restored entries`(){
		context.expecting {
			oneOf(linker).linkEntryToArticles(entry)
			will(returnValue(entry))

			oneOf(eventBus).publish(EntryLinkedEvent(entry))
		}

		linkingWorkflow.onEntryRestored(
			EntityRestoredEvent(entry, Entry::class.java)
		)
	}

	@Test
	fun `Ignores non-entry restoration events`(){
		context.expecting {
			never(linker).linkEntryToArticles(entry)

			never(eventBus).publish(EntryLinkedEvent(entry))
		}

		linkingWorkflow.onEntryRestored(
			EntityRestoredEvent(article, Article::class.java)
		)
	}

	private fun Mockery.expecting(block: Expectations.() -> Unit){
	        checking(Expectations().apply(block))
	}
}