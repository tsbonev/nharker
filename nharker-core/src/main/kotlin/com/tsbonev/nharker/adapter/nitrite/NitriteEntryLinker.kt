package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleSynonymProvider
import com.tsbonev.nharker.core.Articles
import com.tsbonev.nharker.core.Entries
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryLinker

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteEntryLinker(
	private val entries: Entries,
	private val articles: Articles,
	private val synonyms: ArticleSynonymProvider
) : EntryLinker {
	override fun refreshLinksOfArticle(article: Article): List<Entry> {
		val updatedEntryList = mutableListOf<Entry>()

		article.entries.raw().forEach { entryId, _ ->
			val possibleEntry = entries.getById(entryId)
			if (possibleEntry.isPresent) {
				updatedEntryList.add(linkEntryToArticles(possibleEntry.get()))
			}
		}

		return updatedEntryList
	}

	override fun linkEntryToArticles(entry: Entry): Entry {
		val linkedEntry = entry
			.clearExplicitLinksFromContent()
			.runFullTextLinking()
			.clearImplicitLinksFromContent()
			.runSynonymLinking()
			.restoreOriginalContentFrom(entry)

		return entries.save(linkedEntry)
	}

	/**
	 * Removes the explicit links from an article to avoid conflicts when merging,
	 * an entry's explicit links are separate from its implicit ones.
	 */
	private fun Entry.clearExplicitLinksFromContent(): Entry {
		var content = this.content
		this.explicitLinks.keys.forEach {
			content = content.replace(it, "")
		}

		return this.copy(content = content)
	}

	/**
	 * Performs a full text search on all Articles using the
	 * full content of the passed Entry. If this list contains
	 * any articles then their title are matched
	 * against the content of the Entry and saved as Phrase to Id
	 * pairs if an exact match is found.
	 */
	private fun Entry.runFullTextLinking(): Entry {
		val phraseToIdMap = this.implicitLinks.toMutableMap()

		articles.searchByFullTitle(this.content)
			.forEach {
				if (this.content.contains(Regex(it.title, RegexOption.IGNORE_CASE))) {
					phraseToIdMap[it.title] = it.id
				}
			}

		return this.copy(implicitLinks = phraseToIdMap)
	}

	/**
	 * Clears the found implicit links from an entry's content.
	 */
	private fun Entry.clearImplicitLinksFromContent(): Entry {
		var content = this.content
		this.implicitLinks.keys.forEach {
			content = content.replace(it, "")
		}

		return this.copy(content = content)
	}

	/**
	 * Links an entry to articles by looking at the global
	 * synonym map and matching any of the synonyms to the
	 * given entry's content.
	 */
	private fun Entry.runSynonymLinking(): Entry {
		val phraseToIdMap = this.implicitLinks.toMutableMap()

		synonyms.getSynonymMap().forEach { phrase, id ->
			if (this.content.contains(Regex(phrase, RegexOption.IGNORE_CASE))) {
				phraseToIdMap[phrase] = id
			}
		}

		return this.copy(implicitLinks = phraseToIdMap)
	}

	/**
	 * Restores the content of an entry by copying that of a passed entry.
	 *
	 * @param entry The original entry.
	 */
	private fun Entry.restoreOriginalContentFrom(entry: Entry): Entry {
		return this.copy(content = entry.content)
	}
}