package com.tsbonev.nharker.cqrs.core

import org.slf4j.LoggerFactory

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class SimpleEventBus : EventBus {

    private val logger = LoggerFactory.getLogger("SimpleEventBus")

    /**
     * Registered command handlers.
     */
    private val commandHandlers = mutableMapOf<String, CommandHandler<Command>>()

    /**
     * Registered event handlers.
     */
    private val eventHandlers = mutableMapOf<String, MutableList<EventHandler<Event>>>()

    override fun <T : Command> registerCommandHandler(commandClass: Class<T>, commandHandler: CommandHandler<T>) {
        val key = commandClass.name

        @Suppress("UNCHECKED_CAST")
        commandHandlers[key] = commandHandler as CommandHandler<Command>
    }

    override fun <T : Event> registerEventHandler(eventClass: Class<T>, eventHandler: EventHandler<T>) {
        val key = eventClass.name

        if (!eventHandlers.containsKey(key)) {
            eventHandlers[key] = mutableListOf()
        }

        @Suppress("UNCHECKED_CAST")
        eventHandlers[key]!!.add(eventHandler as EventHandler<Event>)
    }

    override fun <T : Command> send(command: T) {
        val key = command::class.java.name

        val commandHandler = commandHandlers[key]

        if (commandHandler == null) {
            logger.warn("No handler registered for $key command class")
        } else {
            commandHandler.handle(command)
        }
    }

    override fun handle(event: Event) {
        val key = event::class.java.name

        val handlers = eventHandlers[key]
                ?: return logger.warn("No handlers registered for $key event class")

        handlers.forEach {
            it.handle(event)
        }
    }
}