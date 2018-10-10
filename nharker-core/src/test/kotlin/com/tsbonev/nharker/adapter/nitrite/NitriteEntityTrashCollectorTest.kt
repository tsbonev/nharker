@file:Suppress("UNCHECKED_CAST")

package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleProperties
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
            "::entryId::",
            date,
            content = "::content::"
    )

    private val trashedEntry = Entry(
            "::trashedEntryId::",
            date,
            content = "::content::"
    )

    private val article = Article(
            "::articleId::",
            "article-id",
            "Article id",
            date,
            properties = ArticleProperties(mutableMapOf("::property::" to entry))
    )

    private lateinit var trashedId: String

    private val entryTrashCollectionName = "Test_entries_trash"

    private val trashCollector = NitriteEntityTrashCollector(db, entryTrashCollectionName)

    private val coll = db.getCollection(entryTrashCollectionName)

    @Before
    fun setUp() {
        val trashedDoc = trashedEntry.toDocument()
        trashedId = trashedDoc.get("entityId", String::class.java)

        coll.insert(trashedDoc)
    }

    @Test
    fun `Trash entity`() {
        val trashedID = trashCollector.trash(entry)

        assertThat(findEntity(trashedID) as Entry, Is(entry))
    }

    @Test
    fun `Trash entities of differing types`() {
        val entryTrashedId = trashCollector.trash(entry)
        val articleTrashedId = trashCollector.trash(article)

        assertThat(findEntity(articleTrashedId) as Article, Is(article))
        assertThat(findEntity(entryTrashedId) as Entry, Is(entry))
    }

    @Test
    fun `Restore entity from trash`() {
        val restoredEntry = trashCollector.restore(trashedId) as Entry

        assertThat(restoredEntry, Is(trashedEntry))
    }

    @Test
    fun `Restoring entity removes it from trash`() {
        trashCollector.restore(trashedId)

        assertThat(coll.find("entityId" eq trashedId).firstOrNull(), Is(nullValue()))
    }

    @Test(expected = EntityNotInTrashException::class)
    fun `Restoring non-trashed entity throws exception`() {
        trashCollector.restore(entry.id)
    }

    @Test
    fun `View trashed entities`() {
        val trashed = trashCollector.view()

        assertThat(trashed, Is(listOf<Any>(trashedEntry)))
    }

    @Test
    fun `Clear trashed`() {
        trashCollector.clear()

        assertThat(coll.find().toList(), Is(emptyList()))
    }

    private fun findEntity(id: String): Any {
        return coll.find("entityId" eq id).first().toEntity()
    }
}