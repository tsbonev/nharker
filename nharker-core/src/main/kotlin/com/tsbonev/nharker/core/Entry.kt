package com.tsbonev.nharker.core

import org.dizitart.no2.IndexType
import org.dizitart.no2.objects.Id
import org.dizitart.no2.objects.Index
import org.dizitart.no2.objects.Indices
import java.time.LocalDateTime

/**
 * Entries are the main wrapper of information,
 * all entries are owned by an article and contain
 * a set of phrases that should point to other articles.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
@Indices(Index(value = "content", type = IndexType.Fulltext))
data class Entry (@Id val id: String,
                  val creationDate: LocalDateTime,
                  val content: String,
                  val links: Map<String, String> = emptyMap())

class EntryNotFoundException : Exception()
