package com.tsbonev.nharker.cqrs.core

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat
import org.slf4j.LoggerFactory

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

    private val workflow = object : Workflow {

        @EventHandler
        fun handle(event: TestEvent) {
            logger.info(event.data)
        }

        @EventHandler
        fun handleAgain(event: TestEvent) {
            logger.info("Handled event twice ${testEvent.data}")
        }

        @CommandHandler
        fun handle(command: TestCommand) {
            logger.info(command.data)
        }

        fun randomFunction(){
            print("This tests that non-annotated functions aren't bothered")
        }
    }

    private val eventBus = SimpleEventBus()

    private val log = ByteArrayOutputStream()

    @Before
    fun setUp() {
        System.setOut(PrintStream(log))
        eventBus.registerWorkflow(workflow)
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
    fun `Register multiple workflows`() {
        data class SecondTestCommand(val data: String) : Command
        data class SecondTestEvent(val data: String) : Event

        val workflow = object : Workflow {
            @CommandHandler
            fun handle(command: SecondTestCommand) {
                logger.info("Parallel workflow received command ${command.data}")
            }

            @EventHandler
            fun handle(event: SecondTestEvent) {
                logger.info("Parallel workflow received event ${event.data}")
            }
        }

        eventBus.registerWorkflow(workflow)

        eventBus.send(SecondTestCommand("::new-command-data::"))

        assertThat(log.toString().contains("INFO"), Is(true))
        assertThat(log.toString()
                .contains("Parallel workflow received command ::new-command-data::"),
                Is(true))

        eventBus.handle(SecondTestEvent("::new-event-data::"))

        assertThat(log.toString().contains("INFO"), Is(true))
        assertThat(log.toString()
                .contains("Parallel workflow received event ::new-event-data::"),
                Is(true))
    }

    @Test
    fun `Handle event multiple times`() {
        eventBus.handle(testEvent)

        assertThat(log.toString().contains("INFO"), Is(true))
        assertThat(log.toString()
                .contains("Handled event twice ${testEvent.data}"),
                Is(true))
    }

    @Test(expected = NoHandlersInWorkflowException::class)
    fun `Registering empty handler throws exception`() {
        val workflow = object : Workflow {}

        eventBus.registerWorkflow(workflow)
    }

    @Test(expected = CommandAlreadyHandledException::class)
    fun `Registering command twice throws exception`() {
        val workflow = object : Workflow {
            @CommandHandler
            fun handle(command: TestCommand) {
            }
        }

        eventBus.registerWorkflow(workflow)
    }

    @Test(expected = IllegalHandlerInWorkflowException::class)
    fun `Registering no-argument handler in workflow throws exception`() {
        val workflow = object : Workflow {
            @CommandHandler
            fun handle() {
            }
        }

        eventBus.registerWorkflow(workflow)
    }

    @Test(expected = IllegalHandlerInWorkflowException::class)
    fun `Registering handler with more than one parameter throws exception`(){
        val workflow = object : Workflow {
            @EventHandler
            fun handle(p1: String, event: Event) {
            }
        }

        eventBus.registerWorkflow(workflow)
    }

    @Test(expected = IllegalHandlerInWorkflowException::class)
    fun `Registering non-event or non-command handler in workflow throws exception`() {
        val workflow = object : Workflow {
            @CommandHandler
            fun handle(p1: String) {
            }
        }

        eventBus.registerWorkflow(workflow)
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