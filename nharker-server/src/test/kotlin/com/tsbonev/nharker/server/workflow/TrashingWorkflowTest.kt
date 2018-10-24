@file:Suppress("UNCHECKED_CAST")

package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.core.Entity
import com.tsbonev.nharker.core.EntityCannotBeCastException
import com.tsbonev.nharker.core.EntityNotInTrashException
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.TrashCollector
import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.cqrs.StatusCode
import com.tsbonev.nharker.server.helpers.ExceptionLogger
import org.jmock.AbstractExpectations
import org.jmock.AbstractExpectations.returnValue
import org.jmock.AbstractExpectations.throwException
import org.jmock.Expectations
import org.jmock.Mockery
import org.jmock.integration.junit4.JUnitRuleMockery
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class TrashingWorkflowTest {
    @Rule
    @JvmField
    val context: JUnitRuleMockery = JUnitRuleMockery()

    private val exceptionLogger = ExceptionLogger()

    private val date = LocalDateTime.ofEpochSecond(1, 1, ZoneOffset.UTC)

    private val entry = Entry(
            "::entry-id::",
            date,
            "::article-id::",
            "::content::"
    )

    private val article = Article(
            "::article-id::",
            "link-title",
            "Full title",
            date
    )

    private val catalogue = Catalogue(
            "::catalogue-id::",
            "::catalogue-title::",
            date
    )

    private val eventBus = context.mock(EventBus::class.java)

    private val trashCollector = context.mock(TrashCollector::class.java)

    private val trashingWorkflow = TrashingWorkflow(eventBus, trashCollector, exceptionLogger)

    @Test
    fun `Trashes entities`() {
        val trashedEntityId = "::trashed-entity-id::"
        val trashedArticleId = "::trashed-article-id::"
        val trashedCatalogueId = "::trashed-catalogue-id::"

        context.expecting {
            oneOf(trashCollector).trash(entry)
            will(AbstractExpectations.returnValue(trashedEntityId))

            oneOf(trashCollector).trash(article)
            will(AbstractExpectations.returnValue(trashedArticleId))

            oneOf(trashCollector).trash(catalogue)
            will(AbstractExpectations.returnValue(trashedCatalogueId))
        }

        trashingWorkflow.onEntryDeleted(EntryDeletedEvent(entry))
        trashingWorkflow.onArticleDeleted(ArticleDeletedEvent(article))
        trashingWorkflow.onCatalogueDeleted(CatalogueDeletedEvent(catalogue))
    }

    @Test
    fun `Restores trashed entity`() {
        context.expecting {
            oneOf(trashCollector).restore("::entity-id::", Entry::class.java)
            will(returnValue(entry))

            oneOf(eventBus).publish(EntityRestoredEvent(entry, Entry::class.java))
        }

        val response = trashingWorkflow.restoreEntity(
                RestoreTrashedEntityCommand("::entity-id::", Entry::class.java))

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Entry, Is(entry))
    }

    @Test
    fun `Restoring non-existing entity returns not found`() {
        context.expecting {
            oneOf(trashCollector).restore("::entity-id::", Entry::class.java)
            will(throwException(EntityNotInTrashException("::entity-id::", Entry::class.java)))
        }

        val response = trashingWorkflow.restoreEntity(
                RestoreTrashedEntityCommand("::entity-id::", Entry::class.java))

        assertThat(response.statusCode, Is(StatusCode.NotFound))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Restoring entity with wrong class returns bad request`() {
        context.expecting {
            oneOf(trashCollector).restore("::entity-id::", Entry::class.java)
            will(throwException(EntityCannotBeCastException("::entity-id::", Entry::class.java)))
        }

        val response = trashingWorkflow.restoreEntity(
                RestoreTrashedEntityCommand("::entity-id::", Entry::class.java))

        assertThat(response.statusCode, Is(StatusCode.BadRequest))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Clearing trash store returns cleared entities`() {
        val entityList = listOf(entry, article, catalogue)

        context.expecting {
            oneOf(trashCollector).view()
            will(returnValue(entityList))

            oneOf(trashCollector).clear()

            oneOf(eventBus).publish(TrashStoreClearedEvent(entityList))
        }

        val response = trashingWorkflow.clearTrashStore(ClearTrashStoreCommand())

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as List<Entity>, Is(entityList))
    }

    @Test
    fun `Retrieves trashed entities`() {
        val entryList = listOf(entry)

        context.expecting {
            oneOf(trashCollector).view()
            will(returnValue(entryList))
        }

        val response = trashingWorkflow.viewTrashedEntities(
                ViewTrashedEntitiesQuery(Entry::class.java))

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as List<Entry>, Is(entryList))
    }

    private fun Mockery.expecting(block: Expectations.() -> Unit) {
        checking(Expectations().apply(block))
    }
}