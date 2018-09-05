package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryRequest
import com.tsbonev.nharker.core.exceptions.EntryAlreadyInArticleException
import com.tsbonev.nharker.core.exceptions.EntryNotFoundException
import org.dizitart.kno2.nitrite
import org.junit.Test
import java.time.LocalDateTime
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat
import org.junit.Before

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteEntriesTest {

    private val db = nitrite { }

    private val entryRequest = EntryRequest(
            "::content::",
            "::articleId::",
            mapOf("::content::" to "::article::")
    )

    private val instant = LocalDateTime.of(1, 1, 1, 1, 1, 1)
    private val collectionName = "TestEntries"

    private val entry = Entry(
            "::entryId::",
            instant,
            "::articleId::",
            "::content::",
            mapOf("::content::" to "::article::")
    )

    private val entries = NitriteEntries(db, collectionName = collectionName) {instant}

    @Before
    fun setUp(){
        db.getRepository(collectionName, Entry::class.java).insert(entry)
    }

    @Test
    fun `Create and return entry`(){
        assertThat(entries.create(entryRequest).copy(id = "::entryId::"), Is(entry))
    }

    @Test
    fun `Retrieve entry by id`(){
        val retrievedEntry = entries.getById("::entryId::")

        assertThat(retrievedEntry.isPresent, Is(true))
        assertThat(retrievedEntry.get(), Is(entry))
    }

    @Test
    fun `Return empty when an entry is not found by id`(){
        val retrievedEntry = entries.getById("::fake-entry-id::")

        assertThat(retrievedEntry.isPresent, Is(false))
    }

    @Test
    fun `Update entry content`(){
        val content = "::new-content::"

        entries.updateContent(entry.id, content)

        val retrievedEntry = entries.getById("::entryId::")

        assertThat(retrievedEntry.get(), Is(entry.copy(content = content)))
    }

    @Test(expected = EntryNotFoundException::class)
    fun `Updating a non-existent entry's content throws exception`(){
        entries.updateContent("::fake-entry-id::", "::content::")
    }

    @Test
    fun `Update entry links`(){
        val links = mapOf("::new-link::" to "::new-article::")

        entries.updateLinks(entry.id, links)

        val retrievedEntry = entries.getById("::entryId::")

        assertThat(retrievedEntry.get(), Is(entry.copy(links = links)))
    }

    @Test(expected = EntryNotFoundException::class)
    fun `Updating a non-existent entry's links throws exception`(){
        entries.updateLinks("::fake-entry-id::", mapOf("::new-link::" to "::new-article::"))
    }

    @Test
    fun `Change entry article`(){
        val updatedEntry = entries.changeArticle(entry.id, "::new-article::")

        assertThat(updatedEntry, Is(entry.copy(articleId = "::new-article::")))
    }

    @Test(expected = EntryNotFoundException::class)
    fun `Changing entry article of non-existent entry throws exception`(){
        entries.changeArticle("::fake-entry-id::", entry.articleId)
    }

    @Test(expected = EntryAlreadyInArticleException::class)
    fun `Changing entry article with the same article throws exception`(){
        entries.changeArticle(entry.id, entry.articleId)
    }

    @Test
    fun `Query entries by content`(){

        val firstEntry = entries.create(entryRequest.copy(content = "apples"))
        val secondEntry = entries.create(entryRequest.copy(content = "apples and oranges"))
        val thirdEntry = entries.create(entryRequest.copy(content = "only oranges"))

        val retrievedByApples = entries.getByContent("apples")
        val retrievedByOranges = entries.getByContent("oranges")

        val appleList = listOf(firstEntry, secondEntry).sortedBy { it.content }
        val orangeList = listOf(secondEntry, thirdEntry).sortedBy { it.content }

        assertThat(retrievedByApples.size, Is(2))
        assertThat(retrievedByApples.sortedBy { it.content }, Is(appleList))

        assertThat(retrievedByOranges.size, Is(2))
        assertThat(retrievedByOranges.sortedBy { it.content }, Is(orangeList))
    }

    @Test
    fun `Delete and return entry`(){
        val deletedEntry = entries.delete(entry.id)

        assertThat(deletedEntry, Is(entry))
        assertThat(entries.getById(entry.id).isPresent, Is(false))
    }

    @Test(expected = EntryNotFoundException::class)
    fun `Deleting non-existent entry throws exception`(){
        entries.delete("::fake-entry-id::")
    }

}