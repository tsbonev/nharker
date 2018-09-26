package com.tsbonev.nharker.core

import org.dizitart.no2.IndexType
import org.dizitart.no2.objects.Id
import org.dizitart.no2.objects.Index
import org.dizitart.no2.objects.Indices
import java.time.LocalDateTime

/**
 * Articles are the main building block of
 * NHarker's organization scheme, they group
 * entries together into different information
 * categories and consolidate their links into
 * one accessible map.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
@Indices(Index(value = "linkTitle", type = IndexType.NonUnique),
        Index(value = "fullTitle", type = IndexType.Fulltext))
data class Article(@Id val id: String,
                   val linkTitle: String,
                   val fullTitle: String,
                   val creationDate: LocalDateTime,
                   val properties: ArticleProperties = ArticleProperties(),
                   val entries: Map<String, Int> = emptyMap(),
                   val links: ArticleLinks = ArticleLinks(mutableMapOf()))

/**
 * Converts a full text title to a lowercase, dash-concatenated string.
 */
fun String.toLinkTitle(): String {
    return this.toLowerCase()
            .replace(' ', '-')
            .replace("\'", "")
            .replace(",", "")
            .replace(".", "")
}

/**
 * Projection used for faster automatic linking.
 */
data class ArticleLinkTitle(val linkTitle: String)

class ArticleNotFoundException : Exception()
class ArticleTitleTakenException : Exception()

class EntryAlreadyInArticleException : Exception()
class EntryNotInArticleException : Exception()