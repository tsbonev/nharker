package com.tsbonev.nharker.core

import com.tsbonev.nharker.core.helpers.ReferenceId
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
        Index(value = "fullTitle", type = IndexType.Fulltext),
        Index(value = "catalogueId", type = IndexType.NonUnique))
data class Article(@Id val id: String,
                   val linkTitle: String,
                   val fullTitle: String,
                   val creationDate: LocalDateTime,
                   val catalogueId: String = ReferenceId.None.value,
                   val properties: List<ArticleProperty> = emptyList(),
                   val entries: Map<String, Int> = emptyMap(),
                   val links: ArticleLinks = ArticleLinks(mutableMapOf()))

/**
 * Converts a full text title to a lowercase, dash-concatenated string.
 */
fun String.toLinkTitle(): String {
    return this.toLowerCase().replace(' ', '-').replace("\'", "")
}

/**
 * Projection used to extract the link titles of an Article.
 */
data class ArticleLinkTitle(val linkTitle: String)

/**
 * Projection used to extract the full title of an Article.
 */
data class ArticleFullTitle(val fullTitle: String)

/**
 * Projection used to extract the general properties of an Article.
 */
data class ArticleHead(val id: String,
                       val linkTitle: String,
                       val fullTitle: String,
                       val catalogue: String,
                       val creationDate: LocalDateTime)