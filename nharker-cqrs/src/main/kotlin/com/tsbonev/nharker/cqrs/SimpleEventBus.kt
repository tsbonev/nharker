package com.tsbonev.nharker.cqrs

import org.slf4j.LoggerFactory
import java.lang.reflect.Method

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class SimpleEventBus : EventBus {
    private val logger = LoggerFactory.getLogger("SimpleEventBus")

    /**
     * Registered command handlers.
     */
    private val commandHandlers = mutableMapOf<String, CommandInvoker>()

    /**
     * Registered event handlers.
     */
    private val eventHandlers = mutableMapOf<String, MutableList<EventInvoker>>()

    private val interceptors = mutableSetOf<Interceptor>()

    override fun registerWorkflow(workflow: Workflow) {
        val methods = workflow::class.java.declaredMethods

        val declaredCommandHandlers = mutableMapOf<String, Method>()
        val declaredEventHandlers = mutableMapOf<String, MutableList<Method>>()

        methods.forEach {
            val parameterTypes = it.parameterTypes

            /**
             * Add the method as a command handler.
             */
            if (it.isAnnotationPresent(CommandHandler::class.java)) {
                validateMethod(it)
                val handlerName = parameterTypes[0].name

                if (commandHandlers[handlerName] != null)
                    throw CommandAlreadyHandledException("${it.name} tries to handle a command that has" +
                            " already been handled!")

                declaredCommandHandlers[handlerName] = it
            }
            /**
             * Add the method as an event handler.
             */
            else if (it.isAnnotationPresent(EventHandler::class.java)) {
                validateMethod(it)
                val handlerName = parameterTypes[0].name

                if (declaredEventHandlers[handlerName] == null)
                    declaredEventHandlers[handlerName] = mutableListOf()

                declaredEventHandlers[handlerName]!!.add(it)
            }
        }

        /**
         * Workflow has not declared any event or command handlers.
         */
        if (declaredCommandHandlers.isEmpty() && declaredEventHandlers.isEmpty())
            throw NoHandlersInWorkflowException("${workflow::class.java.name} does not declare any" +
                    " command or event handlers!")

        declaredCommandHandlers.forEach {
            commandHandlers[it.key] = CommandInvoker(it.value, workflow)
        }

        declaredEventHandlers.forEach { key, value ->
            if (eventHandlers[key] == null) eventHandlers[key] = mutableListOf()

            value.forEach {
                eventHandlers[key]!!.add(EventInvoker(it, workflow))
            }
        }
    }

    override fun registerInterceptor(interceptor: Interceptor) {
        interceptors.add(interceptor)
    }

    override fun <T : Command> send(command: T) {
        val key = command::class.java.name

        val commandHandler = commandHandlers[key]

        if (commandHandler == null) {
            logger.warn("No handler registered for $key command class")
        } else {
            interceptors.forEach{
                it.intercept(command)
            }

            commandHandler.invoke(command)
        }
    }

    override fun handle(event: Event) {
        val key = event::class.java.name

        val handlers = eventHandlers[key]
                ?: return logger.warn("No handlers registered for $key event class")

        interceptors.forEach{
            it.intercept(event)
        }

        handlers.forEach {
            it.invoke(event)
        }
    }

    /**
     * Validates a method's declared event and command handlers' signatures.
     */
    private fun validateMethod(method: Method) {
        val parameterTypes = method.parameterTypes

        /**
         * Method has no or too many parameters to be a handler.
         */
        if (parameterTypes.isEmpty() || parameterTypes.count() > 1)
            throw IllegalHandlerInWorkflowException("${method.name} contains an invalid handler!")

        /**
         * Method requires a non-event or non-command parameter when labeled as a command or event handler.
         */
        if ((!Event::class.java.isAssignableFrom(parameterTypes[0])
                        && method.isAnnotationPresent(EventHandler::class.java))
                ||
                (!Command::class.java.isAssignableFrom(parameterTypes[0])
                        && method.isAnnotationPresent(CommandHandler::class.java)))
            throw IllegalHandlerInWorkflowException("${method.name} contains an invalid handler!")
    }
}