package com.tsbonev.nharker.cqrs.core

import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.hamcrest.CoreMatchers.`is` as Is

class SimpleEventBusTest {

    private val logger = LoggerFactory.getLogger("SimpleEventBusTest")

    private data class TestCommand(val data: String) : Command
    private data class TestEvent(val data: String) : Event

    private data class TestNoHandlerCommand(val data: String) : Command
    private data class TestNoHandlerEvent(val data: String) : Event

    private val testCommand = TestCommand("::command-data::")
    private val testEvent = TestEvent("::event-data::")

    private val testNoHandlerCommand = TestNoHandlerCommand("::command-data::")
    private val testNoHandlerEvent = TestNoHandlerEvent("::event-data::")

    private val testEventHandler = object : EventHandler<TestEvent> {
        override fun handle(event: TestEvent) {
            logger.info(event.data)
        }
    }

    private val anotherTestEventHandler = object : EventHandler<TestEvent> {
        override fun handle(event: TestEvent) {
            logger.info("Handled event twice ${event.data}")
        }
    }

    private val testCommandHandler = object : CommandHandler<TestCommand> {
        override fun handle(command: TestCommand) {
            logger.info(command.data)
        }
    }

    private val eventBus = SimpleEventBus()

    private val log = ByteArrayOutputStream()

    @Before
    fun setUp() {
        System.setOut(PrintStream(log))

        eventBus.registerCommandHandler(TestCommand::class.java, testCommandHandler)
        eventBus.registerEventHandler(TestEvent::class.java, testEventHandler)
    }

    @After
    fun cleanUp() {
        System.setOut(System.out)
    }

    @Test
    fun `Send and handle command`() {
        eventBus.send(testCommand)

        assertThat(log.toString().contains("INFO"), Is(true))
        assertThat(log.toString()
                .contains(testCommand.data),
                Is(true))
    }

    @Test
    fun `Handle event`() {
        eventBus.handle(testEvent)

        assertThat(log.toString().contains("INFO"), Is(true))
        assertThat(log.toString()
                .contains(testEvent.data),
                Is(true))
    }

    @Test
    fun `Handle event multiple times`() {
        eventBus.registerEventHandler(TestEvent::class.java, anotherTestEventHandler)

        eventBus.handle(testEvent)

        assertThat(log.toString().contains("INFO"), Is(true))
        assertThat(log.toString()
                .contains("Handled event twice ${testEvent.data}"),
                Is(true))
    }

    @Test
    fun `Handling event with no registered handlers logs warning`() {
        eventBus.handle(testNoHandlerEvent)

        assertThat(log.toString().contains("WARN"), Is(true))
        assertThat(log.toString()
                .contains("No handlers registered for ${testNoHandlerEvent::class.java.name} event class"),
                Is(true))
    }

    @Test
    fun `Handling command with no registered handler logs warning`() {
        eventBus.send(testNoHandlerCommand)

        assertThat(log.toString().contains("WARN"), Is(true))
        assertThat(log.toString()
                .contains("No handler registered for ${testNoHandlerCommand::class.java.name} command class"),
                Is(true))
    }
}