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

class CatalogueNotFoundException(val catalogueId: String) : Throwable()
class CatalogueTitleTakenException(val catalogueTitle: String) : Throwable()

class CatalogueAlreadyAChildException(val parentCatalogueId: String, val childCatalogueId: String) : Throwable()
class CatalogueNotAChildException(val parentCatalogueId: String, val childCatalogueId: String) : Throwable()
class SelfContainedCatalogueException(val catalogueId: String) : Throwable()
class CatalogueCircularInheritanceException(val parentCatalogueId: String, val childCatalogueId: String) : Throwable()

class ArticleAlreadyInCatalogueException(val catalogueId: String, val articleId: String) : Throwable()
class ArticleNotInCatalogueException(val catalogueId: String, val articleId: String) : Throwable()
