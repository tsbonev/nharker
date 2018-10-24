package com.tsbonev.nharker.core

import org.dizitart.no2.IndexType
import org.dizitart.no2.objects.Id
import org.dizitart.no2.objects.Index
import org.dizitart.no2.objects.Indices
import java.time.LocalDateTime

/**
 * Articles are the main building block of
 * NHarker's organization scheme, they keep track
 * of entries and handle automatically linking
 * to each other based on each entry's content, as
 * well as keeping track of the catalogues they are a part of.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
@Indices(Index(value = "linkTitle", type = IndexType.NonUnique),
        Index(value = "fullTitle", type = IndexType.Fulltext))
data class Article(@Id override val id: String,
                   val linkTitle: String,
                   val fullTitle: String,
                   override val creationDate: LocalDateTime,
                   val catalogues: Set<String> = emptySet(),
                   val properties: ArticleProperties = ArticleProperties(),
                   val entries: OrderedReferenceMap = OrderedReferenceMap(),
                   val links: ArticleLinks = ArticleLinks()) : Entity

/**
 * Converts a full text title to a lowercase, dash-concatenated string.
 */
fun String.toLinkTitle(): String {
    return this.toLowerCase()
            .replace("\\s+".toRegex(), "-")
            .replace("\'", "")
            .replace(":", "")
            .replace(",", "")
            .replace(".", "")
}

/**
 * Projection used for faster automatic linking.
 */
data class ArticleLinkTitle(val id: String, val linkTitle: String)

/**
 * Projection used for retrieving full titles.
 */
data class ArticleFullTitle(val id: String, val fullTitle: String)

class ArticleNotFoundException(val articleId: String) : Throwable()
class ArticleTitleTakenException(val articleTitle: String) : Throwable()

class EntryAlreadyInArticleException(val entryId: String, val articleId: String) : Throwable()
class EntryNotInArticleException(val entryId: String, val articleId: String) : Throwable()