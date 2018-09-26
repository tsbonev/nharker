package com.tsbonev.nharker.core

/**
 * Provides the methods necessary to automatically link
 * articles by looking up their entries' content.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface EntryLinker {
    /**
     * Searches for links in an entry.
     *
     * @param entry The entry to link.
     * @param articleLinkTitles The titles of the articles to try and link to.
     * @return The list of article titles that are linked.
     */
    fun findLinksInContent(entry: Entry, articleLinkTitles: List<String>): List<String>
}