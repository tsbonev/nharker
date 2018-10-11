package com.tsbonev.nharker.core

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
@Indices(Index(value = "parentId", type = IndexType.NonUnique),
        Index(value = "title", type = IndexType.Unique))
data class Catalogue(@Id val id: String,
                     val title: String,
                     val creationDate: LocalDateTime,
                     val articles: Map<String, Int> = emptyMap(),
                     val subCatalogues: Map<String, Int> = emptyMap(),
                     val parentId: String? = null)

class CatalogueNotFoundException : Throwable()
class CatalogueTitleTakenException : Throwable()

class CatalogueAlreadyAChildException : Throwable()
class CatalogueNotAChildException : Throwable()
class SelfContainedCatalogueException : Throwable()
class CatalogueCircularInheritanceException : Throwable()

class ArticleAlreadyInCatalogueException : Throwable()
class ArticleNotInCatalogueException : Throwable()
