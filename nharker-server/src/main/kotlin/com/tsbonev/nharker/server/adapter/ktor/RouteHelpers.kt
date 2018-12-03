package com.tsbonev.nharker.server.adapter.ktor

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.cqrs.CommandResponse
import com.tsbonev.nharker.cqrs.StatusCode
import com.tsbonev.nharker.cqrs.isSuccess
import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond

/**
 * Extension functions that aid when dealing with ktor routes
 * and translating their responses.
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */


/**
 * Convert a CommandResponse payload to an object.
 */
fun CommandResponse.asEntry(): Entry {
	return this.payload.get() as Entry
}

fun CommandResponse.asArticle(): Article {
	return this.payload.get() as Article
}

fun CommandResponse.asCatalogue(): Catalogue {
	return this.payload.get() as Catalogue
}


/**
 * Converts a [StatusCode] to an [HttpStatusCode] object.
 */
fun StatusCode.toHttpStatus(): HttpStatusCode {
	return HttpStatusCode.fromValue(this.value)
}

/**
 * Asserts that a [CommandResponse] has been successful and has
 * a payload to offer.
 */
fun CommandResponse.assertIsSuccessfulRetrieval(): Boolean {
	return this.statusCode.isSuccess() && this.payload.isPresent
}

/**
 * Responds to a [CommandResponse] by converting the [StatusCode] to [HttpStatusCode]
 * and returns its payload, if any is present.
 */
suspend fun ApplicationCall.respondToCommand(commandResponse: CommandResponse) {
	this.respond(commandResponse.statusCode.toHttpStatus(), commandResponse.rawPayload ?: "")
}

/**
 * Retrieves a parameter by a given name or throws an exception.
 *
 * @param paramName The name of the parameter to find.
 * @return The found parameter's value.
 *
 * @exception ParameterNotFoundException thrown when the parameter is not found.
 */
fun ApplicationCall.getParameter(paramName: String): String {
	return this.parameters[paramName] ?: throw ParameterNotFoundException(paramName)
}

class ParameterNotFoundException(val paramName: String) : Throwable()