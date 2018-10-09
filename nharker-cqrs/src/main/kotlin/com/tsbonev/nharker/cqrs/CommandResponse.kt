package com.tsbonev.nharker.cqrs

import java.util.Optional

/**
 * An object that Command Handlers return to clue in the caller
 * what has happened with their command.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class CommandResponse(val statusCode: Int,
                           val rawPayload: Any? = null) {
    val payload: Optional<Any> =
            if (rawPayload == null) Optional.empty()
            else Optional.of(rawPayload)
}