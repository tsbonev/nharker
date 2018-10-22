package com.tsbonev.nharker.core

import java.util.Optional

/**
 * Provides the methods to handle entries in persistence.
 *
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
     * Deletes an entry.
     *
     * @param entryId The Id of the entry targeted.
     * @return The deleted entry.
     */
    @Throws(EntryNotFoundException::class)
    fun delete(entryId: String): Entry

    /**
     * Updates the content of an entry.
     *
     * @param entryId The id of the entry targeted.
     * @param content The new content of the entry.
     * @return The updated entry.
     */
    @Throws(EntryNotFoundException::class)
    fun updateContent(entryId: String, content: String): Entry

    /**
     * Updates the links of an entry.
     *
     * @param entryId The id of the entry targeted.
     * @param links The new links of the entry.
     * @return The updated entry.
     */
    @Throws(EntryNotFoundException::class)
    fun updateLinks(entryId: String, links: Map<String, String>): Entry

    /**
     * Changes the parent article of an entry.
     *
     * @param entryId The id of the entry.
     * @param article The new article that holds the entry.
     *
     * @return The updated entry.
     */
    @Throws(EntryNotFoundException::class)
    fun changeArticle(entryId: String, article: Article): Entry

    /**
     * Retrieves an optional entry by id.
     *
     * @param entryId The id of the entry sought.
     * @return An optional entry.
     */
    fun getById(entryId: String): Optional<Entry>

    /**
     * Retrieves a list of entries matching a text search.
     *
     * @param searchText The text to search by.
     * @return A list of matching entries.
     */
    fun getByContent(searchText: String): List<Entry>
}