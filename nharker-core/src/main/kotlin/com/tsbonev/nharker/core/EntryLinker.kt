package com.tsbonev.nharker.core

/**
 * Provides the methods necessary to automatically link
 * articles by looking up their entries' content.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface EntryLinker {
	/**
	 * Links an entry to articles based on its content.
	 *
	 * @param entry The entry to link.
	 * @return The linked entry.
	 */
	fun linkEntryToArticles(entry: Entry): Entry

	/**
	 * Refreshes the links of every entry in a given article.
	 *
	 * @param article The article whose entries to refresh.
	 * @return A list of updated entries.
	 */
	fun refreshLinksOfArticle(article: Article): List<Entry>
}