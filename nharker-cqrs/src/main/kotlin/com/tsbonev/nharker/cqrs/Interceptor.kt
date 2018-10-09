package com.tsbonev.nharker.cqrs

/**
 * Provides the method to intercept an event or command that
 * the event bus is about to process.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface Interceptor {
    /**
     * Intercepts a command.
     *
     * @param command The command to intercept.
     */
    fun intercept(command: Command)

    /**
     * Intercepts an event.
     *
     * @param event The event to intercept.
     */
    fun intercept(event: Event)
}