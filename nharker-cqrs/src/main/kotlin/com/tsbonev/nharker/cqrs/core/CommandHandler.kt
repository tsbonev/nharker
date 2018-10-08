package com.tsbonev.nharker.cqrs.core

/**
 * An annotation marking a method as a command handler.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class CommandHandler