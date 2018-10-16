package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.Entries
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryNotFoundException
import com.tsbonev.nharker.core.EntryRequest
import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.cqrs.StatusCode
import com.tsbonev.nharker.server.helpers.ExceptionLogger
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

@Suppress("UNCHECKED_CAST")
/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class EntryWorkflowTest {

    @Rule
    @JvmField
    val context: JUnitRuleMockery = JUnitRuleMockery()

    private val eventBus = context.mock(EventBus::class.java)
    private val entries = context.mock(Entries::class.java)

    private val exceptionLogger = ExceptionLogger()

    private val entryWorkflow = EntryWorkflow(eventBus, entries, exceptionLogger)

    private val entryRequest = EntryRequest("::content::",
            emptyMap())

    private val entry = Entry("::entryId::",
            LocalDateTime.now(),
            "::content::",
            emptyMap())

    @Test
    fun `Creating an entry returns it`() {
        context.expecting {
            oneOf(entries).create(entryRequest)
            will(returnValue(entry))

            oneOf(eventBus).publish(EntryCreatedEvent(entry))
        }

        val response = entryWorkflow.createEntry(
                CreateEntryCommand(
                        entryRequest
                )
        )

        assertThat(response.statusCode, Is(StatusCode.Created))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Entry, Is(entry))
    }

    @Test
    fun `Deleting an entry returns it`() {
        context.expecting {
            oneOf(entries).delete(entry.id)
            will(returnValue(entry))

            oneOf(eventBus).publish(EntryDeletedEvent(entry))
        }

        val response = entryWorkflow.deleteEntry(
                DeleteEntryCommand(
                        entry.id
                )
        )

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Entry, Is(entry))
    }

    @Test
    fun `Deleting a non-existing entry returns not found`() {
        context.expecting {
            oneOf(entries).delete(entry.id)
            will(throwException(EntryNotFoundException(entry.id)))
        }

        val response = entryWorkflow.deleteEntry(
                DeleteEntryCommand(
                        entry.id
                )
        )

        assertThat(response.statusCode, Is(StatusCode.NotFound))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Updating an entry's content returns the entry`() {
        val newContent = "::new-content::"

        context.expecting {
            oneOf(entries).updateContent(entry.id, newContent)
            will(returnValue(entry))

            oneOf(eventBus).publish(EntryUpdatedEvent(entry))
        }

        val response = entryWorkflow.updateEntryContent(
                UpdateEntryContentCommand(
                        entry.id, newContent
                )
        )

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Entry, Is(entry))
    }

    @Test
    fun `Updating non-existent entry returns not found`() {
        val newContent = "::new-content::"

        context.expecting {
            oneOf(entries).updateContent(entry.id, newContent)
            will(throwException(EntryNotFoundException(entry.id)))
        }

        val response = entryWorkflow.updateEntryContent(
                UpdateEntryContentCommand(
                        entry.id, newContent
                )
        )

        assertThat(response.statusCode, Is(StatusCode.NotFound))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Updating entry links returns entry`() {
        val newLinks = mapOf("::content::" to "::link::")

        context.expecting {
            oneOf(entries).updateLinks(entry.id, newLinks)
            will(returnValue(entry))

            oneOf(eventBus).publish(EntryUpdatedEvent(entry))
        }

        val response = entryWorkflow.updateEntryLinks(
                UpdateEntryLinksCommand(
                        entry.id, newLinks
                )
        )

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Entry, Is(entry))
    }

    @Test
    fun `Updating entry links of non-existing entry returns not found`() {
        val newLinks = mapOf("::content::" to "::link::")

        context.expecting {
            oneOf(entries).updateLinks(entry.id, newLinks)
            will(throwException(EntryNotFoundException(entry.id)))
        }

        val response = entryWorkflow.updateEntryLinks(
                UpdateEntryLinksCommand(
                        entry.id, newLinks
                )
        )

        assertThat(response.statusCode, Is(StatusCode.NotFound))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Retrieve entry by id`() {
        context.expecting {
            oneOf(entries).getById(entry.id)
            will(returnValue(Optional.of(entry)))
        }

        val response = entryWorkflow.getEntryById(
                GetEntryByIdCommand(
                        entry.id
                )
        )

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Entry, Is(entry))
    }

    @Test
    fun `Retrieving a non-existent entry returns not found`() {
        context.expecting {
            oneOf(entries).getById(entry.id)
            will(returnValue(Optional.empty<Entry>()))
        }

        val response = entryWorkflow.getEntryById(
                GetEntryByIdCommand(
                        entry.id
                )
        )

        assertThat(response.statusCode, Is(StatusCode.NotFound))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Search returns entries that match`() {
        val entryList = listOf(entry)
        val searchString = "::search::"

        context.expecting {
            oneOf(entries).getByContent(searchString)
            will(returnValue(entryList))
        }

        val response = entryWorkflow.searchEntryContent(
                SearchEntriesByContentCommand(
                        searchString
                )
        )

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as List<Entry>, Is(entryList))
    }

    private fun Mockery.expecting(block: Expectations.() -> Unit) {
        checking(Expectations().apply(block))
    }
}