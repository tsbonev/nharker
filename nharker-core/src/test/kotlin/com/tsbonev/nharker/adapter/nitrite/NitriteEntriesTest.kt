package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryNotFoundException
import com.tsbonev.nharker.core.EntryRequest
import com.tsbonev.nharker.core.SortBy
import com.tsbonev.nharker.core.helpers.StubClock
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.nitrite
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteEntriesTest {
	private val db = nitrite { }

	private val date = LocalDateTime.ofInstant(Instant.ofEpochSecond(1), ZoneOffset.UTC)
	private val stubClock = StubClock()

	private val collectionName = "Test_entries"

	private val entryRequest = EntryRequest(
		"::content::",
		"::article-id::",
		mapOf("::phrase::" to "::article-id::")
	)

	private val entry = Entry(
		"::entry-id::",
		date,
		"::article-id::",
		"::content::",
		mapOf("::phrase::" to "::article-id::")
	)

	private val article = Article(
		"::article-id::",
		"Article title",
		date
	)

	private val presavedEntry: Entry
		get() = db.getRepository(collectionName, Entry::class.java)
			.find(Entry::id eq entry.id).first()

	private val entries = NitriteEntries(db, collectionName, stubClock)

	@Before
	fun setUp() {
		db.getRepository(collectionName, Entry::class.java).insert(entry)
	}

	@Test
	fun `Creating entry returns it`() {
		assertThat(entries.create(entryRequest).copy(id = "::entry-id::"), Is(entry))
	}

	@Test
	fun `Saving entry returns it`() {
		val savedEntry = entries.save(entry)

		assertThat(presavedEntry, Is(savedEntry))
	}

	@Test
	fun `Retrieves entry by id`() {
		val retrievedEntry = entries.getById("::entry-id::")

		assertThat(retrievedEntry.isPresent, Is(true))
		assertThat(retrievedEntry.get(), Is(entry))
	}

	@Test
	fun `Returns empty when an entry is not found by id`() {
		val retrievedEntry = entries.getById("::fake-entry-id::")

		assertThat(retrievedEntry.isPresent, Is(false))
	}

	@Test
	fun `Updates entry content`() {
		val content = "::new-content::"

		entries.updateContent(entry.id, content)

		assertThat(presavedEntry, Is(entry.copy(content = content)))
	}

	@Test(expected = EntryNotFoundException::class)
	fun `Updating a non-existing entry's content throws exception`() {
		entries.updateContent("::fake-entry-id::", "::content::")
	}

	@Test
	fun `Updates entry links`() {
		val links = mapOf("::new-link::" to "::new-article::")

		entries.updateLinks(entry.id, links)

		assertThat(presavedEntry, Is(entry.copy(explicitLinks = links)))
	}

	@Test(expected = EntryNotFoundException::class)
	fun `Updating a non-existing entry's links throws exception`() {
		entries.updateLinks("::fake-entry-id::", mapOf("::new-link::" to "::new-article::"))
	}

	@Test
	fun `Changes entry article`() {
		val updatedEntry = entries.changeArticle(entry.id, article.copy("::new-article-id::"))

		assertThat(presavedEntry, Is(entry.copy(articleId = "::new-article-id::")))
		assertThat(presavedEntry, Is(updatedEntry))
	}

	@Test(expected = EntryNotFoundException::class)
	fun `Changing the article of a non-existing entry throws exception`() {
		entries.changeArticle("::non-existing-entry::", article)
	}

	@Test
	fun `Retrieves entries by content`() {
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
	fun `Deleting an entry returns it`() {
		val deletedEntry = entries.delete(entry.id)

		assertThat(entries.getById(entry.id).isPresent, Is(false))
		assertThat(deletedEntry, Is(entry))
	}

	@Test(expected = EntryNotFoundException::class)
	fun `Deleting non-existing entry throws exception`() {
		entries.delete("::fake-entry-id::")
	}
}