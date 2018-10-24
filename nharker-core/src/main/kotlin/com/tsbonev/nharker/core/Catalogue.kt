package com.tsbonev.nharker.core

import org.dizitart.no2.IndexType
import org.dizitart.no2.objects.Id
import org.dizitart.no2.objects.Index
import org.dizitart.no2.objects.Indices
import java.time.LocalDateTime

/**
 * Catalogues are objects that together build an
 * inheritance tree. Catalogues are not concerned
 * with the state of articles.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
@Indices(Index(value = "parentId", type = IndexType.NonUnique),
        Index(value = "title", type = IndexType.Unique))
data class Catalogue(@Id override val id: String,
                     val title: String,
                     override val creationDate: LocalDateTime,
                     val children: OrderedReferenceMap = OrderedReferenceMap(),
                     val parentId: String? = null) : Entity

class CatalogueNotFoundException(val catalogueId: String) : Throwable()
class CatalogueTitleTakenException(val catalogueTitle: String) : Throwable()

class CatalogueAlreadyAChildException(val parentCatalogueId: String, val childCatalogueId: String) : Throwable()
class CatalogueNotAChildException(val parentCatalogueId: String, val childCatalogueId: String) : Throwable()
class SelfContainedCatalogueException(val catalogueId: String) : Throwable()
class CatalogueCircularInheritanceException(val parentCatalogueId: String, val childCatalogueId: String) : Throwable()
