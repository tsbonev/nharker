package com.tsbonev.nharker.core

typealias ArticleId = String
typealias TimesMentioned = Int

/**
 * Provides the methods to modify a mutable map by
 * keeping track of how many times a link has been mentioned.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class ArticleLinks(private val links: MutableMap<ArticleId, TimesMentioned> = mutableMapOf()) {

    /**
     * Returns the amount of times an article has been mentioned, if any.
     *
     * @param articleId The id of the article.
     * @return A nullable integer.
     */
    fun get(articleId: String): Int? {
        return links[articleId]
    }

    /**
     * Checks whether an article is mentioned.
     *
     * @param articleId The article id to check.
     * @return Whether or not it is mentioned.
     */
    fun contains(articleId: String): Boolean {
        return links[articleId] != null
    }

    /**
     * Adds a link to the map, incrementing its mentioned counter.
     *
     * @param articleId The id to add.
     */
    fun addLink(articleId: String) {
        links[articleId] = (links[articleId] ?: 0) + 1
    }

    /**
     * Removes a link mention from the map, decrementing its mentioned counter
     * and removing it completely if it reaches zero.
     *
     * @param articleId The id to decrement.
     */
    fun removeLink(articleId: String) {
        if (contains(articleId)) {
            val timesMentioned = links[articleId]!! - 1
            if (timesMentioned <= 0) links.remove(articleId)
            else links[articleId] = timesMentioned
        }
    }
}