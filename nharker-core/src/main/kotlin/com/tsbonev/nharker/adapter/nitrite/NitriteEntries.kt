package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Entries
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryRequest
import com.tsbonev.nharker.core.exceptions.EntryAlreadyInArticleException
import com.tsbonev.nharker.core.exceptions.EntryNotFoundException
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.filters.text
import org.dizitart.no2.Nitrite
import org.dizitart.no2.NitriteId
import org.dizitart.no2.objects.ObjectRepository
import java.time.LocalDateTime
import java.util.Optional

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteEntries(private val nitriteDb: Nitrite,
                     private val collectionName: String = "Entries",
                     private val getInstant: () -> LocalDateTime = { LocalDateTime.now()}) : Entries {

    /**
     * Retrieve the repository on every request.
     */
    private val coll: ObjectRepository<Entry>
        get() = nitriteDb.getRepository(collectionName, Entry::class.java)

    override fun create(entryRequest: EntryRequest): Entry {
        val entry = Entry(
                NitriteId.newId().toString(),
                getInstant(),
                entryRequest.articleId,
                entryRequest.content,
                entryRequest.links
        )

        coll.insert(entry)
        return entry
    }

    override fun getById(entryId: String): Optional<Entry> {
        val entry = coll.find(Entry::id eq entryId).firstOrNull() ?: return Optional.empty()
        return Optional.of(entry)
    }

    override fun getByContent(searchText: String): List<Entry> {
        return coll.find(Entry::content text searchText).toList()
    }

    override fun updateContent(entryId: String, content: String): Entry {
        val entry = findByIdOrThrow(entryId)
        val updatedEntry = entry.copy(content = content)

        coll.update(updatedEntry)
        return updatedEntry
    }

    override fun updateLinks(entryId: String, links: Map<String, String>): Entry {
        val entry = findByIdOrThrow(entryId)
        val updatedEntry = entry.copy(links = links)

        coll.update(updatedEntry)
        return updatedEntry
    }

    override fun changeArticle(entryId: String, articleId: String): Entry {
        val entry = findByIdOrThrow(entryId)
        if(entry.articleId == articleId) throw EntryAlreadyInArticleException()

        val updatedEntry = entry.copy(articleId = articleId)
        coll.update(updatedEntry)
        return updatedEntry
    }

    override fun delete(entryId: String): Entry {
        val entry = findByIdOrThrow(entryId)
        coll.remove(Entry::id eq entryId)
        return entry
    }

    private fun findByIdOrThrow(entryId: String): Entry{
        return coll.find(Entry::id eq entryId).firstOrNull() ?: throw EntryNotFoundException()
    }
}