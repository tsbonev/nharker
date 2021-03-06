package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.adapter.nitrite.helpers.generateNitriteUniqueId
import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.Entries
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryNotFoundException
import com.tsbonev.nharker.core.EntryRequest
import com.tsbonev.nharker.core.IdGenerator
import com.tsbonev.nharker.core.UUIDGenerator
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.filters.text
import org.dizitart.no2.Nitrite
import org.dizitart.no2.objects.ObjectRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.Optional

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteEntries(
	private val nitriteDb: Nitrite,
	private val collectionName: String = "Entries",
	private val clock: Clock = Clock.systemUTC(),
	private val idGenerator: IdGenerator = UUIDGenerator()
) : Entries {
	private val repo: ObjectRepository<Entry>
		get() = nitriteDb.getRepository(collectionName, Entry::class.java)

	override fun create(entryRequest: EntryRequest): Entry {
		val entry = Entry(
			idGenerator.generateNitriteUniqueId(repo),
			LocalDateTime.now(clock),
			entryRequest.articleId,
			entryRequest.content,
			entryRequest.links
		)

		repo.insert(entry)
		return entry
	}

	override fun save(entry: Entry): Entry {
		repo.update(entry, true)
		return entry
	}

	override fun getById(entryId: String): Optional<Entry> {
		val entry = repo.find(Entry::id eq entryId).firstOrNull()
			?: return Optional.empty()

		return Optional.of(entry)
	}

	override fun getByContent(searchText: String): List<Entry> {
		return repo.find(Entry::content text searchText).toList()
	}

	override fun updateContent(entryId: String, content: String): Entry {
		val entry = findByIdOrThrow(entryId)
		val updatedEntry = entry.copy(content = content)

		repo.update(updatedEntry)
		return updatedEntry
	}

	override fun updateLinks(entryId: String, links: Map<String, String>): Entry {
		val entry = findByIdOrThrow(entryId)
		val updatedEntry = entry.copy(explicitLinks = links)

		repo.update(updatedEntry)
		return updatedEntry
	}

	override fun changeArticle(entryId: String, article: Article): Entry {
		val entry = findByIdOrThrow(entryId)
		val updatedEntry = entry.copy(articleId = article.id)

		repo.update(updatedEntry)
		return updatedEntry
	}

	override fun delete(entryId: String): Entry {
		val entry = findByIdOrThrow(entryId)
		repo.remove(Entry::id eq entryId)
		return entry
	}

	/**
	 * Finds an entry by id or throws an exception.
	 *
	 * @param entryId The id to find.
	 * @return The found Entry.
	 *
	 * @exception EntryNotFoundException thrown when the entry is not found.
	 */
	private fun findByIdOrThrow(entryId: String): Entry {
		return repo.find(Entry::id eq entryId).firstOrNull() ?: throw EntryNotFoundException(entryId)
	}
}