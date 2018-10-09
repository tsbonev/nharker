package com.tsbonev.nharker.cqrs

import java.lang.reflect.Method

/**
 * Provides the methods to send commands and register handlers that
 * listen for the events that they produce.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface EventBus {

    /**
     * Registers a workflow object to the event bus by
     * looking up its annotated methods.
     *
     * @param workflow The workflow to register.
     */
    @Throws(IllegalHandlerInWorkflowException::class,
            NoHandlersInWorkflowException::class,
            CommandAlreadyHandledException::class)
    fun registerWorkflow(workflow: Workflow)

    /**
     * Registers an interceptor that will process events and commands before
     * the event bus.
     *
     * @param interceptor The interceptor to register.
     */
    fun registerInterceptor(interceptor: Interceptor)

    /**
     * Executes a command by delegating it to the
     * registered command handler.
     *
     * @param command The command to execute.
     * @return The response of the command.
     */
    fun <T : Command> send(command: T): CommandResponse

    /**
     * Publishes an event by delegating it to all
     * registered event handlers.
     *
     * @param event The event to publish.
     */
    fun publish(event: Event)
}

/**
 * Invokers to store along with the workflow instance
 */
internal class EventInvoker(private val method: Method, private val instance: Workflow) {
    fun invoke(event: Event) {
        method.invoke(instance, event)
    }
}

internal class CommandInvoker(private val method: Method, private val instance: Workflow) {
    fun invoke(command: Command): CommandResponse {
        return method.invoke(instance, command) as CommandResponse
    }
}

/**
 * Exceptions meant to fail in runtime if a workflow is incorrectly defined.
 */
class IllegalHandlerInWorkflowException(message: String) : Throwable(message)

class CommandAlreadyHandledException(message: String) : Throwable(message)
class NoHandlersInWorkflowException(message: String) : Throwable(message)
