package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleSynonymProvider
import com.tsbonev.nharker.core.Articles
import com.tsbonev.nharker.core.Entries
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.OrderedReferenceMap
import org.jmock.AbstractExpectations.returnValue
import org.jmock.Expectations
import org.jmock.Mockery
import org.jmock.integration.junit4.JUnitRuleMockery
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteEntryLinkerTest {
	@Rule
	@JvmField
	val context: JUnitRuleMockery = JUnitRuleMockery()

	private val date = LocalDateTime.ofEpochSecond(1, 1, ZoneOffset.UTC)

	private val entries = context.mock(Entries::class.java)
	private val articles = context.mock(Articles::class.java)
	private val synonyms = context.mock(ArticleSynonymProvider::class.java)

	private val linker = NitriteEntryLinker(entries, articles, synonyms)

	private val entry = Entry(
		"::entry-id::",
		date,
		"::article-id::",
		"Article title some other title and more titles to come."
	)

	private val article = Article(
		"::article-id::",
		"article-title",
		"Article title",
		date,
		entries = OrderedReferenceMap(linkedMapOf(entry.id to 0))
	)

	@Test
	fun `Links entries to articles`() {
		context.expecting {
			oneOf(articles).searchByFullTitle(entry.content)
			will(returnValue(listOf(article)))

			oneOf(synonyms).getSynonymMap()
			will(returnValue(mapOf("some other title" to "::article-id::")))

			oneOf(entries).save(
				entry.copy(
					implicitLinks = mapOf(
						"Article title" to article.id,
						"some other title" to article.id
					)
				)
			)
		}

		linker.linkEntryToArticles(entry)
	}

	@Test
	fun `Refresh entries in article`() {
		context.expecting {
			oneOf(entries).getById(entry.id)
			will(returnValue(Optional.of(entry)))

			oneOf(articles).searchByFullTitle(entry.content)
			will(returnValue(listOf(article)))

			oneOf(synonyms).getSynonymMap()
			will(returnValue(mapOf("some other title" to "::article-id::")))

			oneOf(entries).save(
				entry.copy(
					implicitLinks = mapOf(
						"Article title" to article.id,
						"some other title" to article.id
					)
				)
			)
		}

		linker.refreshLinksOfArticle(article)
	}

	private fun Mockery.expecting(block: Expectations.() -> Unit) {
		checking(Expectations().apply(block))
	}
}