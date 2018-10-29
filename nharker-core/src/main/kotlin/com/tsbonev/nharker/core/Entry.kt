package com.tsbonev.nharker.core

import org.dizitart.no2.IndexType
import org.dizitart.no2.objects.Id
import org.dizitart.no2.objects.Index
import org.dizitart.no2.objects.Indices
import java.time.LocalDateTime

typealias Phrase = String
typealias ArticleId = String

/**
 * Entries are the main wrapper of information,
 * they may contain links that are explicit which
 * will be used by their parent article alongside with
 * their content for article interlinking.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
@Indices(
	Index(value = "content", type = IndexType.Fulltext),
	Index(value = "articleId", type = IndexType.NonUnique)
)
data class Entry(
	@Id override val id: String,
	override val creationDate: LocalDateTime,
	val articleId: String,
	val content: String,
	val explicitLinks: Map<Phrase, ArticleId> = emptyMap(),
	val implicitLinks: Map<Phrase, ArticleId> = emptyMap()
) : Entity

class EntryNotFoundException(val entryId: String) : Throwable()
