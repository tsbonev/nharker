package com.tsbonev.nharker.cqrs.core

/**
 * An implementation handling a single command.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface CommandHandler<in T : Command> {
    fun handle(command: T)
}