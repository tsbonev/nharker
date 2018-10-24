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
@Suppress("SpellCheckingInspection")
class SimpleEntryLinkerTest {

    @Rule
    @JvmField
    val context: JUnitRuleMockery = JUnitRuleMockery()

    private val articleLinkTitles = mapOf(
            "novem-harker" to "::nh-article-id::",
            "key-of-strength" to "::kos-article-id::",
            "norcit" to "::nor-article-id::",
            "conciliator" to "::con-article-id::",
            "college-of-conciliators" to "::coc-article-id::",
            "immortal-souls" to "::imsls-article-id::",
            "immortal-soul" to "::imsl-article-id::",
            "vanessa-strongwill" to "::vnstw-article-id::",
            "caspar-mistblooded" to "::cspmstb-article-id::",
            "realms" to "::realms-article-id::",
            "second-sons" to "::secson-article-id::",
            "harker-family" to "::harfam-article-id::",
            "strongwill-family" to "::stwfam-article-id::",
            "grad-proper" to "::grpr-article-id::",
            "primus-suprima" to "::prsp-article-id::"
    )

    private val explicitEntryLinks = mapOf(
            "The White Stag" to "novem-harker"
    )

    private val content = "Novem Harker, The White Stag, Conciliator, Tiebreaker of the College of Conciliators, " +
            "tutor of Vanessa Strongwill, last heir of the Strongwill family, born in Grad Proper, " +
            "opposite Norcit. He was never the holder of the Key of Strength, unlike his brother - Primus. " +
            "The College of Conciliators is home to conciliators."

    private val entry = Entry(
            "::entry-id::",
            LocalDateTime.now(),
            "::article-id::",
            content,
            explicitEntryLinks
    )

    private val mentionedArticles = mapOf(
            "novem-harker" to "::nh-article-id::",
            "key-of-strength" to "::kos-article-id::",
            "norcit" to "::nor-article-id::",
            "conciliator" to "::con-article-id::",
            "college-of-conciliators" to "::coc-article-id::",
            "vanessa-strongwill" to "::vnstw-article-id::",
            "strongwill-family" to "::stwfam-article-id::",
            "grad-proper" to "::grpr-article-id::",
            "primus-suprima" to "::prsp-article-id::")

    private val synonymMap = mapOf(
            "primus" to "::prsp-article-id::",
            "college" to "::coc-article-id::",
            "harker" to "::nh-article-id::",
            "novem" to "::nh-article-id::"
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

        assertThat(foundArticleLinks.sorted(), Is(mentionedArticles.values.sorted()))
    }

    @Test
    fun `Find no implicit links`() {
        context.expecting {
            oneOf(synonymProvider).getSynonymMap()
            will(returnValue(emptyMap<String, String>()))
        }

        val entry = Entry(
                "::entry-id::",
                LocalDateTime.now(),
                "::article-id::",
                "There are no articles that have any of these words as a link title."
        )

        val articleLinks = entryLinker.findArticleLinks(entry, articleLinkTitles)

        assertThat(articleLinks, Is(emptySet()))
    }

    private fun Mockery.expecting(block: Expectations.() -> Unit) {
        checking(Expectations().apply(block))
    }
}