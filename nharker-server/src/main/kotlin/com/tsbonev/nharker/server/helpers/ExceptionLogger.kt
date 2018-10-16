package com.tsbonev.nharker.server.helpers

import com.tsbonev.nharker.core.ArticleAlreadyInCatalogueException
import com.tsbonev.nharker.core.ArticleNotFoundException
import com.tsbonev.nharker.core.ArticleNotInCatalogueException
import com.tsbonev.nharker.core.ArticleTitleTakenException
import com.tsbonev.nharker.core.CatalogueAlreadyAChildException
import com.tsbonev.nharker.core.CatalogueNotAChildException
import com.tsbonev.nharker.core.CatalogueNotFoundException
import com.tsbonev.nharker.core.CatalogueTitleTakenException
import com.tsbonev.nharker.core.EntryAlreadyInArticleException
import com.tsbonev.nharker.core.EntryNotFoundException
import com.tsbonev.nharker.core.EntryNotInArticleException
import com.tsbonev.nharker.core.PropertyNotFoundException
import com.tsbonev.nharker.core.SelfContainedCatalogueException
import com.tsbonev.nharker.core.SynonymAlreadyTakenException
import com.tsbonev.nharker.core.SynonymNotFoundException
import com.tsbonev.nharker.cqrs.CommandResponse
import com.tsbonev.nharker.cqrs.StatusCode
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
            is ArticleTitleTakenException -> {
                logger.error("There is already an article with the title ${e.articleTitle}!")
                CommandResponse(StatusCode.BadRequest)
            }

            is ArticleNotFoundException -> {
                logger.error("There is no article with id ${e.articleId}!")
                CommandResponse(StatusCode.NotFound)
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

            is ArticleNotInCatalogueException -> {
                logger.error("The article with id ${e.articleId} is not in the catalogue with id ${e.catalogueId}!")
                CommandResponse(StatusCode.BadRequest)
            }

            is ArticleAlreadyInCatalogueException -> {
                logger.error("The article with id ${e.articleId} is already in the catalogue with id ${e.catalogueId}")
                CommandResponse(StatusCode.BadRequest)
            }

            is CatalogueNotAChildException -> {
                logger.error("The catalogue with id ${e.childCatalogueId} is not a child of the catalogue with id ${e.parentCatalogueId}!")
                CommandResponse(StatusCode.BadRequest)
            }

            is SelfContainedCatalogueException -> {
                logger.error("Catalogue with id ${e.catalogueId} cannot contain itself!")
                CommandResponse(StatusCode.BadRequest)
            }

            is CatalogueAlreadyAChildException -> {
                logger.error("The catalogue with id ${e.parentCatalogueId} is already a parent of catalogue ${e.childCatalogueId}!")
                CommandResponse(StatusCode.BadRequest)
            }


            is CatalogueTitleTakenException -> {
                logger.error("There is already a catalogue with the title ${e.catalogueTitle}!")
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

            else -> {
                logger.error("There is no case for an exception of type ${e::class.java.name}")
                throw e
            }
        }
    }
}