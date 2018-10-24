package com.tsbonev.nharker.cqrs

/**
 * An annotation marking a method as an event handler.
 *
 * EventHandlers:
 * Should not return anything.
 * Must have one and only one parameter of type Event.
 * Must be annotated with @EventHandler.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class EventHandler