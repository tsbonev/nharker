package com.tsbonev.nharker.server

import com.tsbonev.nharker.core.EntityNotInTrashException
import com.tsbonev.nharker.core.TrashCollector
import org.jmock.AbstractExpectations.returnValue
import org.jmock.AbstractExpectations.throwException
import org.jmock.Expectations
import org.jmock.Mockery
import org.jmock.integration.junit4.JUnitRuleMockery
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class LoggingTrashHandlerTest {

    @Rule
    @JvmField
    val context: JUnitRuleMockery = JUnitRuleMockery()

    private val trashCollector = context.mock(TrashCollector::class.java)

    private val log = ByteArrayOutputStream()

    private val trashHandler = LoggingTrashHandler(trashCollector)

    @Before
    fun setUp() {
        System.setOut(PrintStream(log))
    }

    @After
    fun cleanUp() {
        System.setOut(System.out)
    }

    @Test
    fun `Trash entity and log its id`() {
        context.expecting {
            oneOf(trashCollector).trash("::entity::")
            will(returnValue("::trashed-id::"))
        }

        val trashedId = trashHandler.trash("::entity::")

        assertThat(trashedId, Is("::trashed-id::"))

        assertThat(log.toString().contains("INFO"), Is(true))
        assertThat(log.toString().contains("::trashed-id::"), Is(true))
    }

    @Test
    fun `Restore trashed entity and log its id`() {
        val trashedString = "::trashed-string::"

        context.expecting {
            oneOf(trashCollector).restore("::entity-id::")
            will(returnValue(trashedString))
        }

        val trashedEntity = trashHandler
                .restore("::entity-id::", String::class.java)

        assertThat(trashedEntity.isPresent, Is(true))
        assertThat(trashedEntity.get(), Is(trashedString))

        assertThat(log.toString().contains("INFO"), Is(true))
        assertThat(log.toString().contains("::entity-id::"), Is(true))
    }

    @Test
    fun `Return empty when not found and log error`() {
        context.expecting {
            oneOf(trashCollector).restore("::entity-id::")
            will(throwException(EntityNotInTrashException()))
        }

        val trashedEntity = trashHandler
                .restore("::entity-id::", String::class.java)

        assertThat(trashedEntity.isPresent, Is(false))

        assertThat(log.toString().contains("ERROR"), Is(true))
        assertThat(log.toString().contains("::entity-id::"), Is(true))
    }

    @Test
    fun `Return empty when failing to cast and log error`() {
        context.expecting {
            oneOf(trashCollector).restore("::entity-id::")
            will(returnValue("::entity::"))
        }

        val trashedEntity = trashHandler
                .restore("::entity-id::", Nothing::class.java)

        assertThat(trashedEntity.isPresent, Is(false))

        assertThat(log.toString().contains("ERROR"), Is(true))
        assertThat(log.toString().contains("::entity-id::"), Is(true))
    }

    @Test
    fun `Return list of trashed entities and log class name and size`() {
        context.expecting {
            oneOf(trashCollector).view()
            will(returnValue(listOf("::first::", "::second::", "::third::", 1, 2, 3)))
        }

        val trashedStrings = trashHandler.view(String::class.java)

        assertThat(trashedStrings.sorted(), Is(listOf("::first::", "::second::", "::third::").sorted()))

        assertThat(log.toString().contains("INFO"), Is(true))
        assertThat(log.toString().contains("Returned a list of ${String::class.java.name}" +
                " with size 3"), Is(true))
    }

    @Test
    fun `Return an empty list of entries when none match the class`() {
        context.expecting {
            oneOf(trashCollector).view()
            will(returnValue(listOf("::first::", "::second::", "::third::", 1, 2, 3)))
        }

        val trashedStrings = trashHandler.view(Double::class.java)

        assertThat(trashedStrings, Is(emptyList()))

        assertThat(log.toString().contains("INFO"), Is(true))
        assertThat(log.toString().contains("Returned a list of ${Double::class.java.name}" +
                " with size 0"), Is(true))
    }

    private fun Mockery.expecting(block: Expectations.() -> Unit) {
        checking(Expectations().apply(block))
    }
}