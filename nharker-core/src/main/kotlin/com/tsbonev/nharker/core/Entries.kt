package com.tsbonev.nharker.core

import com.tsbonev.nharker.core.exceptions.EntryAlreadyInArticleException
import com.tsbonev.nharker.core.exceptions.EntryNotFoundException
import java.util.Optional

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface Entries {

    /**
     * Creates an Entry from an EntryRequest and saves it into persistence.
     *
     * @param entryRequest The request to be converted and saved.
     * @return The created entry.
     */
    fun create(entryRequest: EntryRequest): Entry

    /**
     * Saves an entry into persistence, overwriting the previous one
     * if it exists.
     */
    fun save(entry: Entry): Entry

    /**
     * Updates the content of an entry.
     *
     * @param entryId The value of the entry targeted.
     * @param content The new content of the entry.
     * @return The updated entry.
     */
    @Throws(EntryNotFoundException::class)
    fun updateContent(entryId: String, content: String): Entry

    /**
     * Updates the links of an entry.
     *
     * @param entryId The value of the entry targeted.
     * @param links The new links of the entry.
     * @return The updated entry.
     */
    @Throws(EntryNotFoundException::class)
    fun updateLinks(entryId: String, links: Map<String, String>): Entry

    /**
     * Deletes an entry.
     *
     * @param entryId The Id of the entry targeted.
     * @return The deleted entry.
     */
    @Throws(EntryNotFoundException::class)
    fun delete(entryId: String): Entry

    /**
     * Returns an optional entry by value.
     *
     * @param entryId The value of the entry sought.
     * @return An optional entry.
     */
    fun getById(entryId: String): Optional<Entry>

    /**
     * Returns a list of entries matching a text search.
     *
     * @param searchText The text to search by.
     * @return A list of matching entries.
     */
    fun getByContent(searchText: String): List<Entry>

    /**
     * Changes the article of an entry.
     *
     * @param entryId The value of the entry targeted.
     * @param articleId The value of the new article.
     * @return The updated entry.
     */
    @Throws(EntryNotFoundException::class,
            EntryAlreadyInArticleException::class)
    fun changeArticle(entryId: String, articleId: String): Entry
}