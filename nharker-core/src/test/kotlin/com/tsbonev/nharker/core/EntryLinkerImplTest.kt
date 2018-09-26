package com.tsbonev.nharker.core

import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class EntryLinkerImplTest {

    private val date = LocalDateTime.ofInstant(Instant.ofEpochSecond(1), ZoneOffset.UTC)

    private val explicitLinkText = "purple mangoes"
    private val explicitLinkTitle = "purple-mangoes"

    private val entry = Entry(
            "::entryId::",
            date,
            "John Doe, Amy Doe. Apples, pears, lemons. Bananas and oranges with some purple mangoes.",
            mapOf(explicitLinkText to explicitLinkTitle)
    )

    private val articleLinkTitles = listOf(
            "apples",
            "john-doe",
            "amy-doe",
            "peter-steel",
            "pears",
            explicitLinkTitle,
            *explicitLinkTitle.split(' ').toTypedArray(),
            "lemons",
            "lemon-cake"
    )

    private val entryLinker = EntryLinkerImpl()

    @Test
    fun `Link entry to article titles`(){
        val articleLinks = entryLinker.findLinksInContent(entry, articleLinkTitles)

        assertThat(articleLinks.sortedBy { it }, Is(listOf(
                "john-doe",
                "amy-doe",
                "apples",
                "lemons",
                "pears"
        ).sortedBy { it }))
    }

    @Test
    fun `Remove explicit links before linking implicitly`(){
        val articleLinks = entryLinker.findLinksInContent(entry, articleLinkTitles)

        assertThat(articleLinks.contains(explicitLinkTitle), Is(false))
        assertThat(articleLinks.contains(explicitLinkText.split(' ')[0]), Is(false))
        assertThat(articleLinks.contains(explicitLinkText.split(' ')[1]), Is(false))
    }

    @Test
    fun `Find no implicit links`(){
        val entry = Entry(
                "::entryId::",
                date,
                "There are no articles that have any of these words as a link title."
        )

        val articleLinks = entryLinker.findLinksInContent(entry, articleLinkTitles)

        assertThat(articleLinks, Is(emptyList()))
    }
}