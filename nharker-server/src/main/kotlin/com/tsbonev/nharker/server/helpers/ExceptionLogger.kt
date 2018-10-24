package com.tsbonev.nharker.server.helpers

import com.tsbonev.nharker.core.ArticleNotFoundException
import com.tsbonev.nharker.core.ArticleTitleTakenException
import com.tsbonev.nharker.core.CatalogueAlreadyAChildException
import com.tsbonev.nharker.core.CatalogueCircularInheritanceException
import com.tsbonev.nharker.core.CatalogueNotAChildException
import com.tsbonev.nharker.core.CatalogueNotFoundException
import com.tsbonev.nharker.core.CatalogueTitleTakenException
import com.tsbonev.nharker.core.EntityCannotBeCastException
import com.tsbonev.nharker.core.EntityNotInTrashException
import com.tsbonev.nharker.core.EntryAlreadyInArticleException
import com.tsbonev.nharker.core.EntryNotFoundException
import com.tsbonev.nharker.core.EntryNotInArticleException
import com.tsbonev.nharker.core.PaginationException
import com.tsbonev.nharker.core.PropertyNotFoundException
import com.tsbonev.nharker.core.SelfContainedCatalogueException
import com.tsbonev.nharker.core.SynonymAlreadyTakenException
import com.tsbonev.nharker.core.SynonymNotFoundException
import com.tsbonev.nharker.cqrs.CommandResponse
import com.tsbonev.nharker.cqrs.StatusCode
import com.tsbonev.nharker.server.workflow.NoPaginatorRegisteredException
import org.slf4j.LoggerFactory

/**
 * A universal class that consolidates the messages
 * logged during certain exceptions.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class ExceptionLogger {
    private val logger = LoggerFactory.getLogger("ExceptionLogger")

    /**
     * Logs a response built from the given exception.
     *
     * @param e The Exception to log and build a response from.
     * @return A CommandResponse.
     */
    fun logException(e: Throwable): CommandResponse {
        return when (e) {
            //region Article Exceptions
            is ArticleNotFoundException -> {
                logger.error("There is no article with id ${e.articleId}!")
                CommandResponse(StatusCode.NotFound)
            }

            is ArticleTitleTakenException -> {
                logger.error("There is already an article with the title ${e.articleTitle}!")
                CommandResponse(StatusCode.BadRequest)
            }

            is EntryAlreadyInArticleException -> {
                logger.error("The article with id ${e.articleId} already contains the entry with id ${e.entryId}!")
                CommandResponse(StatusCode.BadRequest)
            }

            is EntryNotInArticleException -> {
                logger.error("The article with id ${e.articleId} does not contain an entry with id ${e.entryId}!")
                CommandResponse(StatusCode.BadRequest)
            }

            is PropertyNotFoundException -> {
                logger.error("Could not find property named ${e.property}!")
                CommandResponse(StatusCode.BadRequest)
            }
            //endregion

            //region Catalogue Exceptions
            is CatalogueNotFoundException -> {
                logger.error("Could not find catalogue with id ${e.catalogueId}!")
                CommandResponse(StatusCode.NotFound)
            }

            is CatalogueTitleTakenException -> {
                logger.error("There is already a catalogue with the title ${e.catalogueTitle}!")
                CommandResponse(StatusCode.BadRequest)
            }

            is CatalogueNotAChildException -> {
                logger.error("The catalogue with id ${e.childCatalogueId} is not a child of the catalogue with id ${e.parentCatalogueId}!")
                CommandResponse(StatusCode.BadRequest)
            }

            is CatalogueAlreadyAChildException -> {
                logger.error("The catalogue with id ${e.parentCatalogueId} is already a parent of catalogue ${e.childCatalogueId}!")
                CommandResponse(StatusCode.BadRequest)
            }

            is CatalogueCircularInheritanceException -> {
                logger.error("Circular inheritance not allowed! Catalogue with id ${e.parentCatalogueId} is a child of catalogue with id ${e.childCatalogueId}!")
                CommandResponse(StatusCode.BadRequest)
            }

            is SelfContainedCatalogueException -> {
                logger.error("Catalogue with id ${e.catalogueId} cannot contain itself!")
                CommandResponse(StatusCode.BadRequest)
            }
            //endregion

            //region Entry Exceptions
            is EntryNotFoundException -> {
                logger.error("There is no entry with id ${e.entryId}!")
                CommandResponse(StatusCode.NotFound)
            }
            //endregion

            //region Synonym Exceptions
            is SynonymAlreadyTakenException -> {
                logger.error("The synonym ${e.synonym} is already present in the synonym map!")
                CommandResponse(StatusCode.BadRequest)
            }

            is SynonymNotFoundException -> {
                logger.error("The synonym ${e.synonym} was not found in the map")
                CommandResponse(StatusCode.NotFound)
            }
            //endregion

            //region Pagination Exceptions
            is PaginationException -> {
                logger.error("Cannot paginate with page of ${e.page} and page siez of ${e.pageSize}!")
                CommandResponse(StatusCode.BadRequest)
            }

            is NoPaginatorRegisteredException -> {
                logger.error("No paginator registered for class ${e.objectType}!")
                CommandResponse(StatusCode.BadRequest)
            }
            //endregion

            //region Trash Exceptions
            is EntityNotInTrashException -> {
                logger.error("Cannot find trashed entity with id ${e.entityId} and class ${e.entityClass}!")
                CommandResponse(StatusCode.NotFound)
            }

            is EntityCannotBeCastException -> {
                logger.error("Cannot cast trashed entity with id ${e.entityId} to class ${e.entityClass}!")
                CommandResponse(StatusCode.BadRequest)
            }
            //endregion

            else -> {
                logger.error("There is no case for an exception of type ${e::class.java.name}")
                logger.error(e.stackTrace.toString())
                e.printStackTrace()
                CommandResponse(StatusCode.InternalServerError)
            }
        }
    }
}