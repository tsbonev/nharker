package com.tsbonev.nharker.cqrs.core

/**
 * An implementation handling a single event.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface EventHandler<in T : Event> {
    fun handle(event: T)
}