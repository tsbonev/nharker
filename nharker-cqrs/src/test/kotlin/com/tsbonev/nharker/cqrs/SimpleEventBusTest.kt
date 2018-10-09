package com.tsbonev.nharker.cqrs

import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.hamcrest.CoreMatchers.`is` as Is

@Suppress("unused")
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
        fun handle(command: TestCommand): CommandResponse {
            return CommandResponse(200,
                    testCommand.data)
        }

        fun randomFunction() {
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
        val commandResponse = eventBus.send(testCommand)

        assertThat(commandResponse.statusCode, Is(200))
        assertThat(commandResponse.payload.isPresent, Is(true))
        assertThat(commandResponse.payload.get() as String, Is(testCommand.data))
    }

    @Test
    fun `Handle event`() {
        eventBus.publish(testEvent)

        assertThat(log.toString().contains("INFO"), Is(true))
        assertThat(log.toString()
                .contains(testEvent.data),
                Is(true))
    }

    @Test
    fun `Register interceptor and intercept event and command`() {
        val interceptor = object : Interceptor {
            override fun intercept(command: Command) {
                logger.info("Intercepted command $command")
            }

            override fun intercept(event: Event) {
                logger.info("Intercepted event $event")
            }
        }

        eventBus.registerInterceptor(interceptor)

        eventBus.send(testCommand)
        assertThat(log.toString().contains("INFO"), Is(true))
        assertThat(log.toString()
                .contains("Intercepted command $testCommand"),
                Is(true))

        eventBus.publish(testEvent)
        assertThat(log.toString().contains("INFO"), Is(true))
        assertThat(log.toString()
                .contains("Intercepted event $testEvent"),
                Is(true))
    }

    @Test
    fun `Register multiple workflows`() {
        data class SecondTestCommand(val data: String) : Command
        data class SecondTestEvent(val data: String) : Event

        val workflow = object : Workflow {
            @CommandHandler
            fun handle(command: SecondTestCommand): CommandResponse {
                return CommandResponse(200,
                        "Parallel workflow received command ${command.data}")
            }

            @EventHandler
            fun handle(event: SecondTestEvent) {
                logger.info("Parallel workflow received event ${event.data}")
            }
        }

        eventBus.registerWorkflow(workflow)

        val commandResponse = eventBus.send(SecondTestCommand("::new-command-data::"))

        assertThat(commandResponse.statusCode, Is(200))
        assertThat(commandResponse.payload.isPresent, Is(true))
        assertThat(commandResponse.payload.get() as String,
                Is("Parallel workflow received command ::new-command-data::"))

        eventBus.publish(SecondTestEvent("::new-event-data::"))

        assertThat(log.toString().contains("INFO"), Is(true))
        assertThat(log.toString()
                .contains("Parallel workflow received event ::new-event-data::"),
                Is(true))
    }

    @Test
    fun `Handle event multiple times`() {
        eventBus.publish(testEvent)

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
            fun handle(command: TestCommand): CommandResponse {
                return CommandResponse(200)
            }
        }

        eventBus.registerWorkflow(workflow)
    }

    @Test(expected = IllegalHandlerInWorkflowException::class)
    fun `Registering no-argument handler in workflow throws exception`() {
        val workflow = object : Workflow {
            @CommandHandler
            fun handle(): CommandResponse {
                return CommandResponse(200)
            }
        }

        eventBus.registerWorkflow(workflow)
    }

    @Test(expected = IllegalHandlerInWorkflowException::class)
    fun `Registering handler with more than one parameter throws exception`() {
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
            fun handle(p1: String): CommandResponse {
                return CommandResponse(200)
            }
        }

        eventBus.registerWorkflow(workflow)
    }

    @Test(expected = IllegalHandlerInWorkflowException::class)
    fun `Registering event handler that does not return CommandResponse throws exception`() {
        val workflow = object : Workflow {
            @CommandHandler
            fun handle(command: Command) {
            }
        }

        eventBus.registerWorkflow(workflow)
    }

    @Test
    fun `Handling event with no registered handlers logs warning`() {
        eventBus.publish(testNoHandlerEvent)

        assertThat(log.toString().contains("WARN"), Is(true))
        assertThat(log.toString()
                .contains("No handlers registered for ${testNoHandlerEvent::class.java.name} event class"),
                Is(true))
    }

    @Test(expected = NoHandlersInWorkflowException::class)
    fun `Handling command with no registered handler throws exception`() {
        eventBus.send(testNoHandlerCommand)
    }
}