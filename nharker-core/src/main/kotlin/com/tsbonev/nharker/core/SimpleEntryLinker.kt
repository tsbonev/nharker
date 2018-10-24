package com.tsbonev.nharker.core

/**
 * An implementation of EntryLinker that contains a list
 * of English stop words to remove from an Entry's content and
 * the private methods to clean up the content further.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class SimpleEntryLinker(private val articleSynonymProvider: ArticleSynonymProvider)
    : EntryLinker {

    private val punctuations = listOf(
            "!", "?", ".", ",", ":", ";",
            "[", "]", "<", ">", "(", ")", "{", "}",
            "\'", "\'", "`", "-"
    )

    private val stopWords = setOf("i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now")

    override fun findArticleLinks(entry: Entry, articleLinkTitlesToIds: Map<String, String>): Set<String> {
        val articleIdSet = mutableSetOf<String>()

        val cleanArticleLinkMap = mutableMapOf<String, String>()

        articleLinkTitlesToIds.forEach { linkTitle, id ->
            cleanArticleLinkMap[linkTitle.removeStopWords("-")
                    .trim()
                    .replace(" ", "-")] = id
        }

        entry.normalizeContent()
                .mapLinks(articleIdSet, cleanArticleLinkMap)
                .mapLinks(articleIdSet, articleSynonymProvider.getSynonymMap())

        return articleIdSet
    }

    /**
     * Maps a string's content from a given map to a given collection.
     *
     * @param linkSet The collection to add the found links to.
     * @param linkMap The map of links to match.
     */
    private fun String.mapLinks(linkSet: MutableSet<String>, linkMap: Map<String, String>): String {
        var contentHolder = this
        linkMap.forEach {
            if (this.contains("-${it.key}-")) {
                contentHolder = this.replace("-${it.key}-", "-#-")
                linkSet.add(it.value)
            }
        }
        return contentHolder
    }

    /**
     * Normalizes an entry's content by removing explicit links,
     * punctuation and stop words, finally concatenating the words with dashes
     * for quick link title matching.
     */
    private fun Entry.normalizeContent(): String {
        val cleanContent = this.removeExplicitLinks()
                .removePunctuations()
                .removeStopWords()

        return "-${cleanContent.trim().replace("\\s+".toRegex(), "-")}-"
    }

    /**
     * Removes the explicit links in an entry from its content.
     */
    private fun Entry.removeExplicitLinks(): String {
        var content = this.content
        val explicitLinks = this.links.keys.toList()

        explicitLinks.forEach {
            content = content.replace(it, "")
        }

        return content
    }

    /**
     * Removes punctuations from a string.
     */
    private fun String.removePunctuations(): String {
        var contentHolder = this

        punctuations.forEach {
            contentHolder = contentHolder.replace(it, "")
        }

        return contentHolder
    }

    /**
     * Removes stop words from a string.
     *
     * @param delimiter The delimiter to split by, whitespace by default.
     */
    private fun String.removeStopWords(delimiter: String = " "): String {
        val contentHolder = this.toLowerCase()
                .split(delimiter)
                .toMutableList()

        val stringBuilder = StringBuilder()

        contentHolder.filter { !stopWords.contains(it) }
                .forEach { stringBuilder.append("$it ") }

        return stringBuilder.toString()
    }
}