@file:Suppress("UNCHECKED_CAST")

package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleProperties
import com.tsbonev.nharker.core.EntityCannotBeCastException
import com.tsbonev.nharker.core.EntityNotInTrashException
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.helpers.toDocument
import com.tsbonev.nharker.core.helpers.toEntity
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.nitrite
import org.hamcrest.CoreMatchers.nullValue
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
class NitriteEntityTrashCollectorTest {
	private val db = nitrite { }

	private val date = LocalDateTime.ofInstant(Instant.ofEpochSecond(1), ZoneOffset.UTC)

	private val entry = Entry(
		"::entry-id::",
		date,
		articleId = "::article-id::",
		content = "::content::"
	)

	private val trashedEntry = Entry(
		"::trashed-entry-id::",
		date,
		"::article-id::",
		content = "::content::"
	)

	private val article = Article(
		"::article-id::",
		"Article title",
		date,
		properties = ArticleProperties(mutableMapOf("::property-name::" to entry.id))
	)


	private val entryTrashCollectionName = "Test_entries_trash"

	private val coll = db.getCollection(entryTrashCollectionName)

	private val trashCollector = NitriteEntityTrashCollector(db, entryTrashCollectionName)

	@Before
	fun setUp() {
		val trashedDoc = trashedEntry.toDocument()
		coll.insert(trashedDoc)
	}

	@Test
	fun `Trashing entity returns its id`() {
		val trashedID = trashCollector.trash(entry)

		assertThat(findEntity(trashedID) as Entry, Is(entry))
	}

	@Test
	fun `Trashes entities of differing types`() {
		val entryTrashedId = trashCollector.trash(entry)
		val articleTrashedId = trashCollector.trash(article)

		assertThat(findEntity(articleTrashedId) as Article, Is(article))
		assertThat(findEntity(entryTrashedId) as Entry, Is(entry))
	}

	@Test
	fun `Restores entity from trash`() {
		val restoredEntry = trashCollector.restore(trashedEntry.id, Entry::class.java)

		assertThat(restoredEntry, Is(trashedEntry))
	}

	@Test
	fun `Restoring entity removes it from trash`() {
		trashCollector.restore(trashedEntry.id, Entry::class.java)

		assertThat(coll.find("entityId" eq trashedEntry.id).firstOrNull(), Is(nullValue()))
	}

	@Test(expected = EntityNotInTrashException::class)
	fun `Restoring non-trashed entity throws exception`() {
		trashCollector.restore(entry.id, Entry::class.java)
	}

	@Test(expected = EntityCannotBeCastException::class)
	fun `Restoring entity and casting it to wrong type throws exception`() {
		trashCollector.restore(trashedEntry.id, Article::class.java)
	}

	@Test
	fun `Retrieves a list of trashed entities`() {
		val trashed = trashCollector.view()

		assertThat(trashed, Is(listOf<Any>(trashedEntry)))
	}

	@Test
	fun `Clears trashed entities`() {
		val entityList = coll.find().toList().map {
			it.toEntity()
		}

		val clearedEntities = trashCollector.clear()

		assertThat(clearedEntities, Is(entityList))
		assertThat(coll.find().toList(), Is(emptyList()))
	}

	private fun findEntity(id: String): Any {
		return coll.find("entityId" eq id).first().toEntity()
	}
}