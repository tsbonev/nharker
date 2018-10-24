package com.tsbonev.nharker.core

/**
 * Provides the methods to persist a map of synonyms that ease article linking.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface ArticleSynonymProvider {
    /**
     * Returns a map of article link synonyms.
     *
     * @return A map of synonym links to real links.
     */
    fun getSynonymMap(): Map<String, String>

    /**
     * Adds a synonym to the global map.
     *
     * @param articleSynonym The synonym to add.
     * @param article The article that the synonym points to
     * the article's id will be saved against the synonym.
     * @return The added synonym.
     */
    @Throws(SynonymAlreadyTakenException::class)
    fun addSynonym(articleSynonym: String, article: Article): String

    /**
     * Removes a synonym from the global map.
     *
     * @param articleSynonym The synonym to remove.
     * @return The removed synonym to id pair.
     */
    @Throws(SynonymNotFoundException::class)
    fun removeSynonym(articleSynonym: String): Pair<String, String>
}

class SynonymNotFoundException(val synonym: String) : Throwable()
class SynonymAlreadyTakenException(val synonym: String) : Throwable()