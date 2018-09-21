package com.tsbonev.nharker.core

import com.tsbonev.nharker.core.helpers.ReferenceId
import org.dizitart.no2.IndexType
import org.dizitart.no2.objects.Id
import org.dizitart.no2.objects.Index
import org.dizitart.no2.objects.Indices
import java.time.LocalDateTime

/**
 * Catalogues are the entity that groups together
 * articles into coherent categories. Catalogues
 * can nest each other and order articles and
 * their nested subcatalogues.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
@Indices(Index(value = "parentCatalogue", type = IndexType.NonUnique),
        Index(value = "title", type = IndexType.Unique))
data class Catalogue(@Id override val id: String,
                     val title: String,
                     val creationDate: LocalDateTime,
                     val articles: Map<String, Int> = emptyMap(),
                     val subCatalogues: Map<String, Int> = emptyMap(),
                     val parentCatalogue: String = ReferenceId.None.value) : Entity(id)

class CatalogueNotFoundException : Exception()
class CatalogueAlreadyAChildException : Exception()
class CatalogueNotAChildException : Exception()
class CatalogueTitleTakenException : Exception()
class SelfContainedCatalogueException : Exception()
