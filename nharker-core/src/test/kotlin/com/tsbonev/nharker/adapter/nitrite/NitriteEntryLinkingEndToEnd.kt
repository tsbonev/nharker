@file:Suppress("SpellCheckingInspection")

package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleRequest
import com.tsbonev.nharker.core.Entry
import org.dizitart.kno2.nitrite
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * Tests entry linking by setting up a fake environment and
 * filling it with articles, then expecting the linker to
 * find the articles that have been mentioned in an entry's content.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteEntryLinkingEndToEnd {
	private val db = nitrite { }

	private val entries = NitriteEntries(db, "Test_entries")
	private val articles = NitriteArticles(db, "Test_articles")
	private val synonyms = NitriteArticleSynonymProvider(db, "Test_synonyms")

	private val catalogues = setOf("::catalogue-id::")

	private val articleRequestList = listOf(
		ArticleRequest("Novem Harker", catalogues),
		ArticleRequest("Vanessa Strongwill", catalogues),
		ArticleRequest("Conciliator", catalogues),
		ArticleRequest("The College of Conciliators", catalogues),
		ArticleRequest("The Key of Strength", catalogues),
		ArticleRequest("Norcit", catalogues),
		ArticleRequest("Immortal Soul", catalogues),
		ArticleRequest("Immortal Souls", catalogues),
		ArticleRequest("Caspar Mistblooded", catalogues),
		ArticleRequest("The Realms", catalogues),
		ArticleRequest("The Second Sons", catalogues),
		ArticleRequest("The Harker Family", catalogues),
		ArticleRequest("The Strongwill Family", catalogues),
		ArticleRequest("Grad Proper", catalogues),
		ArticleRequest("Primus Suprima", catalogues)
	)

	private val articleList = mutableListOf<Article>()

	private val content = "The White Stag, Conciliator, Tiebreaker of the College of Conciliators, " +
			"tutor of Vanessa Strongwill, last heir of the Strongwill family, born in Grad Proper, " +
			"opposite Norcit. He was never the holder of the Key of Strength, unlike his brother - Primus. " +
			"The College of Conciliators is home to conciliators. " +
			"The Realms are the home of magical power."

	private val entry = Entry(
		"::entry-id::",
		LocalDateTime.now(),
		"::article-id::",
		content,
		explicitLinks = mapOf("The Realms" to "::the-realms-article-id::")
	)

	private val mentionedArticleTitles = listOf(
		"Novem Harker",
		"Vanessa Strongwill",
		"The Strongwill Family",
		"The Key of Strength",
		"Norcit",
		"Conciliator",
		"The College of Conciliators",
		"Grad Proper",
		"Primus Suprima"
	)

	private val linker = NitriteEntryLinker(entries, articles, synonyms)

	@Before
	fun setUp() {
		//Save articles
		articleRequestList.forEach {
			articleList.add(
				articles.create(it)
			)
		}

		//Set up synonyms
		val collegeArticle = articleList.find {
			it.title == "The College of Conciliators"
		}
		synonyms.addSynonym("College", collegeArticle!!)

		val primusArticle = articleList.find {
			it.title == "Primus Suprima"
		}
		synonyms.addSynonym("Primus", primusArticle!!)

		val harkerArticle = articleList.find {
			it.title == "Novem Harker"
		}
		synonyms.addSynonym("Harker", harkerArticle!!)
		synonyms.addSynonym("Novem", harkerArticle)
		synonyms.addSynonym("The White Stag", harkerArticle)
	}

	@Test
	fun `Links entry to articles`() {
		val linkedEntry = linker.linkEntryToArticles(entry)

		val mentionedArticles = articleList.filter {
			it.title in mentionedArticleTitles
		}

		val linkedArticles = articleList.filter {
			it.id in linkedEntry.implicitLinks.values
		}

		assertThat(mentionedArticles, Is(linkedArticles))
	}
}