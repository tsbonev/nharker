@file:Suppress("UNCHECKED_CAST")

package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.*
import com.tsbonev.nharker.core.helpers.toDocument
import com.tsbonev.nharker.core.helpers.toEntity
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.nitrite
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat
import org.junit.Before

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
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
            date
    )

    private val entryTrashCollectionName = "Test_entries_trash"

    private val trashCollector = NitriteEntityTrashCollector(db, entryTrashCollectionName)

    private val coll = db.getCollection(entryTrashCollectionName)
    
    @Before
    fun setUp(){
        coll.insert(trashedEntry.toDocument())
    }
    
    @Test
    fun `Trash entity`(){
        trashCollector.trash(entry)

        assertThat(findEntity(entry.id) as Entry, Is(entry))
    }

    @Test
    fun `Trash entities of differing types`(){
        trashCollector.trash(entry)
        trashCollector.trash(article)

        assertThat(findEntity(article.id) as Article, Is(article))
        assertThat(findEntity(entry.id) as Entry, Is(entry))
    }

    @Test(expected = EntityAlreadyInTrashException::class)
    fun `Trashing same entity twice throws exception`(){
        trashCollector.trash(trashedEntry)
    }

    @Test
    fun `Restore entity from trash`(){
        val restoredEntry = trashCollector.restore(trashedEntry.id) as Entry

        assertThat(restoredEntry, Is(trashedEntry))
    }

    @Test
    fun `Restoring entity removes it from trash`(){
        trashCollector.restore(trashedEntry.id)

        assertThat(coll.find("entityId" eq entry.id).firstOrNull(), Is(nullValue()))
    }

    @Test(expected = EntityNotInTrashException::class)
    fun `Restoring non-trashed entity throws exception`(){
        trashCollector.restore(entry.id)
    }

    @Test
    fun `View trashed entities`(){
        val trashed = trashCollector.view() as List<Entry>

        assertThat(trashed, Is(listOf(trashedEntry)))
    }

    @Test
    fun `Clear trashed`(){
        trashCollector.clear()

        assertThat(coll.find().toList(), Is(emptyList()))
    }

    private fun findEntity(id: String): Entity {
        return coll.find("entityId" eq id).first().toEntity()
    }
}