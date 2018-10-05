package com.tsbonev.nharker.cqrs.core

/**
 * Provides the methods to send commands and register handlers that
 * listen for the events that they produce.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface EventBus {

    /**
     * Registers a command handler.
     *
     * @param commandClass The class of command that is handled.
     * @param commandHandler The command handler.
     */
    fun <T : Command> registerCommandHandler(commandClass: Class<T>, commandHandler: CommandHandler<T>)

    /**
     * Registers an event handler.
     *
     * @param eventClass The class of the event that is handled.
     * @param eventHandler The event handler.
     */
    fun <T : Event> registerEventHandler(eventClass: Class<T>, eventHandler: EventHandler<T>)

    /**
     * Executes a command.
     *
     * @param command The command to execute.
     */
    fun <T : Command> send(command: T)

    /**
     * Handles an event with the registered event handlers.
     *
     * @param event The event to handle.
     */
    fun handle(event: Event)
}