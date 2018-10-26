package com.tsbonev.nharker.core

/**
 * Provides the methods necessary to automatically link
 * articles by looking up their entries' content.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface EntryLinker {
	/**
	 * Searches for links in an entry's content.
	 *
	 * @param entry The entry to link from.
	 * @param articleLinkTitlesToIds The link titles of the articles to match for links.
	 * @return The set of article ids that are linked.
	 */
	fun findArticleLinks(entry: Entry, articleLinkTitlesToIds: Map<String, String>): Set<String>
}