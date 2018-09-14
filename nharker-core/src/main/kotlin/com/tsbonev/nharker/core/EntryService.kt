package com.tsbonev.nharker.core

import com.tsbonev.nharker.core.exceptions.EntryAlreadyInArticleException
import com.tsbonev.nharker.core.exceptions.EntryNotFoundException
import java.util.Optional

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface EntryService {
    /**
     * Returns an optional entry by id.
     *
     * @param entryId The id of the entry sought.
     * @return An optional entry.
     */
    fun getById(entryId: String): Optional<Entry>

    /**
     * Changes the article of an entry.
     *
     * @param entryId The id of the entry targeted.
     * @param articleId The id of the new article.
     * @return The updated entry.
     */
    @Throws(EntryNotFoundException::class,
            EntryAlreadyInArticleException::class)
    fun changeArticle(entryId: String, articleId: String): Entry
}