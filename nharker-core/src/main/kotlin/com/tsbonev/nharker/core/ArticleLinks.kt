package com.tsbonev.nharker.core

/**
 * Provides the methods to modify a mutable map by
 * keeping track of how many times a link has been mentioned.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class ArticleLinks(private val links: MutableMap<String, Int> = mutableMapOf()) {

    /**
     * Returns the amount of times an article has been mentioned.
     *
     * @param articleLinkTitle The link title of the article.
     * @return A nullable integer.
     */
    fun get(articleLinkTitle: String): Int? {
        return links[articleLinkTitle]
    }

    /**
     * Checks whether an article is mentioned.
     *
     * @param articleLinkTitle The article to check.
     * @return Whether or not it is mentioned.
     */
    fun contains(articleLinkTitle: String): Boolean {
        return links[articleLinkTitle] != null
    }

    /**
     * Adds a link to the map, incrementing its mentioned counter.
     *
     * @param articleLinkTitle The link to add.
     */
    fun addLink(articleLinkTitle: String) {
        links[articleLinkTitle] = (links[articleLinkTitle] ?: 0) + 1
    }

    /**
     * Removes a link mention from the map, decrementing its mentioned counter
     * and removing it completely if it reaches zero.
     *
     * @param articleLinkTitle The link to decrement.
     */
    fun removeLink(articleLinkTitle: String) {
        if (contains(articleLinkTitle)) {
            val timesMentioned = links[articleLinkTitle]!! - 1
            if (timesMentioned <= 0) links.remove(articleLinkTitle)
            else links[articleLinkTitle] = timesMentioned
        }
    }
}