package com.tsbonev.nharker.core

import org.jmock.AbstractExpectations.returnValue
import org.jmock.Expectations
import org.jmock.Mockery
import org.jmock.integration.junit4.JUnitRuleMockery
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class SimpleEntryLinkerTest {

    @Rule
    @JvmField
    val context: JUnitRuleMockery = JUnitRuleMockery()

    private val articleLinkTitles = listOf(
            "novem-harker",
            "key-of-strength",
            "grad-proper",
            "norcit",
            "conciliator",
            "college-of-conciliators",
            "immortal-souls",
            "immortal-soul",
            "vanessa-strongwill",
            "caspar-mistblooded",
            "realms",
            "second-sons",
            "harker-family",
            "strongwill-family",
            "grad-proper",
            "primus-suprima"
    )

    private val explicitEntryLinks = mapOf(
            "The White Stag" to "novem-harker"
    )

    private val content = "Novem Harker, The White Stag, Conciliator, Tiebreaker of the College of Conciliators, " +
            "tutor of Vanessa Strongwill, last heir of the Strongwill family, born in Grad Proper, " +
            "opposite Norcit. He was never the holder of the Key of Strength, unlike his brother - Primus. " +
            "The College of Conciliators is home to conciliators."

    private val entry = Entry(
            "::entryId::",
            LocalDateTime.now(),
            content,
            explicitEntryLinks
    )

    private val mentionedArticles = setOf("novem-harker",
            "vanessa-strongwill",
            "strongwill-family",
            "college-of-conciliators",
            "conciliator",
            "grad-proper",
            "norcit",
            "key-of-strength",
            "primus-suprima").sorted()

    private val synonymMap = mapOf(
            "primus" to "primus-suprima",
            "college" to "college-of-conciliators",
            "harker" to "novem-harker",
            "novem" to "novem-harker"
    )

    private val synonymProvider = context.mock(ArticleSynonymProvider::class.java)

    private val entryLinker = SimpleEntryLinker(synonymProvider)

    @Test
    fun `Link entry content to articles`() {
        context.expecting {
            oneOf(synonymProvider).getSynonymMap()
            will(returnValue(synonymMap))
        }

        val foundArticleLinks = entryLinker.findArticleLinks(entry, articleLinkTitles)

        assertThat(foundArticleLinks.sorted(), Is(mentionedArticles))
    }

    @Test
    fun `Find no implicit links`() {
        context.expecting {
            oneOf(synonymProvider).getSynonymMap()
            will(returnValue(emptyMap<String, String>()))
        }

        val entry = Entry(
                "::entryId::",
                LocalDateTime.now(),
                "There are no articles that have any of these words as a link title."
        )

        val articleLinks = entryLinker.findArticleLinks(entry, articleLinkTitles)

        assertThat(articleLinks, Is(emptySet()))
    }

    private fun Mockery.expecting(block: Expectations.() -> Unit) {
        checking(Expectations().apply(block))
    }
}