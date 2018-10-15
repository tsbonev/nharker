package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleAlreadyInCatalogueException
import com.tsbonev.nharker.core.ArticleNotInCatalogueException
import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.core.CatalogueAlreadyAChildException
import com.tsbonev.nharker.core.CatalogueNotAChildException
import com.tsbonev.nharker.core.CatalogueNotFoundException
import com.tsbonev.nharker.core.CatalogueRequest
import com.tsbonev.nharker.core.CatalogueTitleTakenException
import com.tsbonev.nharker.core.Catalogues
import com.tsbonev.nharker.core.SelfContainedCatalogueException
import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.server.helpers.HttpStatus
import org.jmock.AbstractExpectations.returnValue
import org.jmock.AbstractExpectations.throwException
import org.jmock.Expectations
import org.jmock.Mockery
import org.jmock.integration.junit4.JUnitRuleMockery
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.util.Optional
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class CatalogueWorkflowTest {

    @Rule
    @JvmField
    val context: JUnitRuleMockery = JUnitRuleMockery()

    private val eventBus = context.mock(EventBus::class.java)
    private val catalogues = context.mock(Catalogues::class.java)

    private val catalogueWorkflow = CatalogueWorkflow(eventBus, catalogues)

    private val catalogueRequest = CatalogueRequest(
            "::catalogue-title::"
    )

    private val catalogue = Catalogue(
            "::id::",
            "::catalogue-title::",
            LocalDateTime.now()
    )

    private val article = Article(
            "::article-id::",
            "full-title",
            "Full title",
            LocalDateTime.now()
    )

    @Test
    fun `Creating a catalogue returns it`() {
        context.expecting {
            oneOf(catalogues).create(catalogueRequest)
            will(returnValue(catalogue))

            oneOf(eventBus).publish(CatalogueCreatedEvent(catalogue))
        }

        val response = catalogueWorkflow.createCatalogue(CreateCatalogueCommand(catalogueRequest))

        assertThat(response.statusCode, Is(HttpStatus.Created.value))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Catalogue, Is(catalogue))
    }

    @Test
    fun `Creating a catalogue with a taken title returns bad request`() {
        context.expecting {
            oneOf(catalogues).create(catalogueRequest)
            will(throwException(CatalogueTitleTakenException()))
        }

        val response = catalogueWorkflow.createCatalogue(CreateCatalogueCommand(catalogueRequest))

        assertThat(response.statusCode, Is(HttpStatus.BadRequest.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Creating a catalogue with a non-existing parent returns bad request`() {
        context.expecting {
            oneOf(catalogues).create(catalogueRequest)
            will(throwException(CatalogueNotFoundException()))
        }

        val response = catalogueWorkflow.createCatalogue(CreateCatalogueCommand(catalogueRequest))

        assertThat(response.statusCode, Is(HttpStatus.BadRequest.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Retrieve catalogue by id`() {
        context.expecting {
            oneOf(catalogues).getById(catalogue.id)
            will(returnValue(Optional.of(catalogue)))
        }

        val response = catalogueWorkflow.getCatalogueById(GetCatalogueByIdCommand(catalogue.id))

        assertThat(response.statusCode, Is(HttpStatus.OK.value))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Catalogue, Is(catalogue))
    }

    @Test
    fun `Retrieving non-existing catalogue returns not found`() {
        context.expecting {
            oneOf(catalogues).getById(catalogue.id)
            will(returnValue(Optional.empty<Catalogue>()))
        }

        val response = catalogueWorkflow.getCatalogueById(GetCatalogueByIdCommand(catalogue.id))

        assertThat(response.statusCode, Is(HttpStatus.NotFound.value))
        assertThat(response.payload.isPresent, Is(false))
    }


    @Test
    fun `Deleting catalogue returns it`() {
        context.expecting {
            oneOf(catalogues).delete(catalogue.id)
            will(returnValue(catalogue))

            oneOf(eventBus).publish(CatalogueDeletedEvent(catalogue))
        }

        val response = catalogueWorkflow.deleteCatalogue(DeleteCatalogueCommand(catalogue.id))

        assertThat(response.statusCode, Is(HttpStatus.OK.value))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Catalogue, Is(catalogue))
    }

    @Test
    fun `Deleting non-existing catalogue returns not found`() {
        context.expecting {
            oneOf(catalogues).delete(catalogue.id)
            will(throwException(CatalogueNotFoundException()))
        }

        val response = catalogueWorkflow.deleteCatalogue(DeleteCatalogueCommand(catalogue.id))

        assertThat(response.statusCode, Is(HttpStatus.NotFound.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Change catalogue title`() {
        context.expecting {
            oneOf(catalogues).changeTitle(catalogue.id, "::new-title::")
            will(returnValue(catalogue))

            oneOf(eventBus).publish(CatalogueUpdatedEvent(catalogue))
        }

        val response = catalogueWorkflow.changeCatalogueTitle(
                ChangeCatalogueTitleCommand(catalogue.id, "::new-title::"))

        assertThat(response.statusCode, Is(HttpStatus.OK.value))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Catalogue, Is(catalogue))
    }

    @Test
    fun `Changing catalogue title to a taken one returns bad request`() {
        context.expecting {
            oneOf(catalogues).changeTitle(catalogue.id, "::new-title::")
            will(throwException(CatalogueTitleTakenException()))
        }

        val response = catalogueWorkflow.changeCatalogueTitle(
                ChangeCatalogueTitleCommand(catalogue.id, "::new-title::"))

        assertThat(response.statusCode, Is(HttpStatus.BadRequest.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Changing title of non-existing catalogue returns not found`() {
        context.expecting {
            oneOf(catalogues).changeTitle(catalogue.id, "::new-title::")
            will(throwException(CatalogueNotFoundException()))
        }

        val response = catalogueWorkflow.changeCatalogueTitle(
                ChangeCatalogueTitleCommand(catalogue.id, "::new-title::"))

        assertThat(response.statusCode, Is(HttpStatus.NotFound.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Change catalogue parent`() {
        context.expecting {
            oneOf(catalogues).changeParentCatalogue(catalogue.id, catalogue)
            will(returnValue(catalogue))

            oneOf(eventBus).publish(CatalogueUpdatedEvent(catalogue))
        }

        val response = catalogueWorkflow.changeCatalogueParent(
                ChangeCatalogueParentCommand(catalogue.id, catalogue))

        assertThat(response.statusCode, Is(HttpStatus.OK.value))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Catalogue, Is(catalogue))
    }

    @Test
    fun `Changing catalogue parent to the same parent returns bad request`() {
        context.expecting {
            oneOf(catalogues).changeParentCatalogue(catalogue.id, catalogue)
            will(throwException(CatalogueAlreadyAChildException()))
        }

        val response = catalogueWorkflow.changeCatalogueParent(
                ChangeCatalogueParentCommand(catalogue.id, catalogue))

        assertThat(response.statusCode, Is(HttpStatus.BadRequest.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Changing parent of non-existing catalogue returns not found`() {
        context.expecting {
            oneOf(catalogues).changeParentCatalogue(catalogue.id, catalogue)
            will(throwException(CatalogueNotFoundException()))
        }

        val response = catalogueWorkflow.changeCatalogueParent(
                ChangeCatalogueParentCommand(catalogue.id, catalogue))

        assertThat(response.statusCode, Is(HttpStatus.NotFound.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Append subcatalogue to catalogue`() {
        context.expecting {
            oneOf(catalogues).appendSubCatalogue(catalogue.id, catalogue)
            will(returnValue(catalogue))

            oneOf(eventBus).publish(CatalogueUpdatedEvent(catalogue))
        }

        val response = catalogueWorkflow.appendSubCatalogue(
                AppendSubCatalogueCommand(catalogue.id, catalogue))

        assertThat(response.statusCode, Is(HttpStatus.OK.value))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Catalogue, Is(catalogue))
    }

    @Test
    fun `Appending catalogue to itself returns bad request`() {
        context.expecting {
            oneOf(catalogues).appendSubCatalogue(catalogue.id, catalogue)
            will(throwException(SelfContainedCatalogueException()))
        }

        val response = catalogueWorkflow.appendSubCatalogue(
                AppendSubCatalogueCommand(catalogue.id, catalogue))

        assertThat(response.statusCode, Is(HttpStatus.BadRequest.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Appending catalogue that is already a child returns bad request`() {
        context.expecting {
            oneOf(catalogues).appendSubCatalogue(catalogue.id, catalogue)
            will(throwException(CatalogueAlreadyAChildException()))
        }

        val response = catalogueWorkflow.appendSubCatalogue(
                AppendSubCatalogueCommand(catalogue.id, catalogue))

        assertThat(response.statusCode, Is(HttpStatus.BadRequest.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Appending subcatalogue to a non-existent catalogue returns not found`() {
        context.expecting {
            oneOf(catalogues).appendSubCatalogue(catalogue.id, catalogue)
            will(throwException(CatalogueNotFoundException()))
        }

        val response = catalogueWorkflow.appendSubCatalogue(
                AppendSubCatalogueCommand(catalogue.id, catalogue))

        assertThat(response.statusCode, Is(HttpStatus.NotFound.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Switch subcatalogue order in catalogue`() {
        context.expecting {
            oneOf(catalogues).switchSubCatalogues(catalogue.id, catalogue, catalogue)
            will(returnValue(catalogue))

            oneOf(eventBus).publish(CatalogueUpdatedEvent(catalogue))
        }

        val response = catalogueWorkflow.switchSubCatalogues(
                SwitchSubCataloguesCommand(catalogue.id, catalogue, catalogue))

        assertThat(response.statusCode, Is(HttpStatus.OK.value))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Catalogue, Is(catalogue))
    }

    @Test
    fun `Switching subcatalogue orders in a catalogue that does not contain both returns bad request`() {
        context.expecting {
            oneOf(catalogues).switchSubCatalogues(catalogue.id, catalogue, catalogue)
            will(throwException(CatalogueNotAChildException()))
        }

        val response = catalogueWorkflow.switchSubCatalogues(
                SwitchSubCataloguesCommand(catalogue.id, catalogue, catalogue))

        assertThat(response.statusCode, Is(HttpStatus.BadRequest.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Switching subcatalogue orders in a non-existing catalogue returns not found`() {
        context.expecting {
            oneOf(catalogues).switchSubCatalogues(catalogue.id, catalogue, catalogue)
            will(throwException(CatalogueNotFoundException()))
        }

        val response = catalogueWorkflow.switchSubCatalogues(
                SwitchSubCataloguesCommand(catalogue.id, catalogue, catalogue))

        assertThat(response.statusCode, Is(HttpStatus.NotFound.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Remove subcatalogue from catalogue`() {
        context.expecting {
            oneOf(catalogues).removeSubCatalogue(catalogue.id, catalogue)
            will(returnValue(catalogue))

            oneOf(eventBus).publish(CatalogueUpdatedEvent(catalogue))
        }

        val response = catalogueWorkflow.removeSubCatalogue(
                RemoveSubCatalogueCommand(catalogue.id, catalogue))

        assertThat(response.statusCode, Is(HttpStatus.OK.value))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Catalogue, Is(catalogue))
    }

    @Test
    fun `Removing subcatalogue from non-parent catalogue returns bad request`() {
        context.expecting {
            oneOf(catalogues).removeSubCatalogue(catalogue.id, catalogue)
            will(throwException(CatalogueNotAChildException()))
        }

        val response = catalogueWorkflow.removeSubCatalogue(
                RemoveSubCatalogueCommand(catalogue.id, catalogue))

        assertThat(response.statusCode, Is(HttpStatus.BadRequest.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Removing subcatalogue from a non-existent catalogue returns not found`() {
        context.expecting {
            oneOf(catalogues).removeSubCatalogue(catalogue.id, catalogue)
            will(throwException(CatalogueNotFoundException()))
        }

        val response = catalogueWorkflow.removeSubCatalogue(
                RemoveSubCatalogueCommand(catalogue.id, catalogue))

        assertThat(response.statusCode, Is(HttpStatus.NotFound.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Append article to catalogue`() {
        context.expecting {
            oneOf(catalogues).appendArticle(catalogue.id, article)
            will(returnValue(catalogue))

            oneOf(eventBus).publish(CatalogueUpdatedEvent(catalogue))
        }

        val response = catalogueWorkflow.appendArticle(
                AppendArticleToCatalogueCommand(catalogue.id, article))

        assertThat(response.statusCode, Is(HttpStatus.OK.value))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Catalogue, Is(catalogue))
    }

    @Test
    fun `Appending article that is already contained in the catalogue returns bad request`() {
        context.expecting {
            oneOf(catalogues).appendArticle(catalogue.id, article)
            will(throwException(ArticleAlreadyInCatalogueException()))
        }

        val response = catalogueWorkflow.appendArticle(
                AppendArticleToCatalogueCommand(catalogue.id, article))

        assertThat(response.statusCode, Is(HttpStatus.BadRequest.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Appending article to a non-existent catalogue returns not found`() {
        context.expecting {
            oneOf(catalogues).appendArticle(catalogue.id, article)
            will(throwException(CatalogueNotFoundException()))
        }

        val response = catalogueWorkflow.appendArticle(
                AppendArticleToCatalogueCommand(catalogue.id, article))

        assertThat(response.statusCode, Is(HttpStatus.NotFound.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Switch article order in catalogue`() {
        context.expecting {
            oneOf(catalogues).switchArticles(catalogue.id, article, article)
            will(returnValue(catalogue))

            oneOf(eventBus).publish(CatalogueUpdatedEvent(catalogue))
        }

        val response = catalogueWorkflow.switchArticlesInCatalogue(
                SwitchArticlesInCatalogueCommand(catalogue.id, article, article))

        assertThat(response.statusCode, Is(HttpStatus.OK.value))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Catalogue, Is(catalogue))
    }

    @Test
    fun `Switching article orders in a catalogue that does not contain both returns bad request`() {
        context.expecting {
            oneOf(catalogues).switchArticles(catalogue.id, article, article)
            will(throwException(ArticleNotInCatalogueException()))
        }

        val response = catalogueWorkflow.switchArticlesInCatalogue(
                SwitchArticlesInCatalogueCommand(catalogue.id, article, article))

        assertThat(response.statusCode, Is(HttpStatus.BadRequest.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Switching article orders in a non-existing catalogue returns not found`() {
        context.expecting {
            oneOf(catalogues).switchArticles(catalogue.id, article, article)
            will(throwException(CatalogueNotFoundException()))
        }

        val response = catalogueWorkflow.switchArticlesInCatalogue(
                SwitchArticlesInCatalogueCommand(catalogue.id, article, article))

        assertThat(response.statusCode, Is(HttpStatus.NotFound.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Remove article from catalogue`() {
        context.expecting {
            oneOf(catalogues).removeArticle(catalogue.id, article)
            will(returnValue(catalogue))

            oneOf(eventBus).publish(CatalogueUpdatedEvent(catalogue))
        }

        val response = catalogueWorkflow.removeArticle(
                RemoveArticleFromCatalogueCommand(catalogue.id, article))

        assertThat(response.statusCode, Is(HttpStatus.OK.value))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Catalogue, Is(catalogue))
    }

    @Test
    fun `Removing article that is not contained in catalogue returns bad request`() {
        context.expecting {
            oneOf(catalogues).removeArticle(catalogue.id, article)
            will(throwException(ArticleNotInCatalogueException()))
        }

        val response = catalogueWorkflow.removeArticle(
                RemoveArticleFromCatalogueCommand(catalogue.id, article))

        assertThat(response.statusCode, Is(HttpStatus.BadRequest.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Removing article from a non-existent catalogue returns not found`() {
        context.expecting {
            oneOf(catalogues).removeArticle(catalogue.id, article)
            will(throwException(CatalogueNotFoundException()))
        }

        val response = catalogueWorkflow.removeArticle(
                RemoveArticleFromCatalogueCommand(catalogue.id, article))

        assertThat(response.statusCode, Is(HttpStatus.NotFound.value))
        assertThat(response.payload.isPresent, Is(false))
    }

    private fun Mockery.expecting(block: Expectations.() -> Unit) {
        checking(Expectations().apply(block))
    }
}