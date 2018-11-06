package com.tsbonev.nharker.server

import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.core.CatalogueRequest
import com.tsbonev.nharker.cqrs.CommandResponse
import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.cqrs.StatusCode
import com.tsbonev.nharker.cqrs.isSuccess
import com.tsbonev.nharker.server.adapter.koin.exceptionLoggingModule
import com.tsbonev.nharker.server.adapter.koin.fakeNitriteDbModule
import com.tsbonev.nharker.server.adapter.koin.nitriteEntryLinkerModule
import com.tsbonev.nharker.server.adapter.koin.nitritePersistenceModule
import com.tsbonev.nharker.server.adapter.koin.simpleCqrsModule
import com.tsbonev.nharker.server.adapter.koin.workflowModule
import com.tsbonev.nharker.server.workflow.CatalogueWorkflow
import com.tsbonev.nharker.server.workflow.ChangeCatalogueParentCommand
import com.tsbonev.nharker.server.workflow.ChangeCatalogueTitleCommand
import com.tsbonev.nharker.server.workflow.CreateCatalogueCommand
import com.tsbonev.nharker.server.workflow.DeleteCatalogueCommand
import com.tsbonev.nharker.server.workflow.GetCatalogueByIdQuery
import com.tsbonev.nharker.server.workflow.OrphanCatalogueCommand
import com.tsbonev.nharker.server.workflow.SwitchChildCataloguesCommand
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.ktor.ext.inject
import org.koin.standalone.StandAloneContext.startKoin

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */

/**
 * Setup function for the Ktor server's endpoints and logic.
 */
fun Application.main() {
	install(DefaultHeaders)
	install(CallLogging)
	install(ContentNegotiation) {
		gson {
			setPrettyPrinting()

			disableHtmlEscaping()
			enableComplexMapKeySerialization()

			serializeNulls()
		}
	}

	val eventBus: EventBus by inject()

	val catalogueWorkflow: CatalogueWorkflow by inject()

	with(eventBus) {
		registerWorkflow(catalogueWorkflow)
	}

	routing {
		route("/catalogue") {
			get("{id}") {
				val response = eventBus.send(
					GetCatalogueByIdQuery(
						catalogueId = call.getParameter("id")
					)
				)

				call.respondToCommand(response)
			}

			post {
				val catalogueRequestDTO = call.receive<CatalogueRequestDTO>()
				val response = eventBus.send(
					CreateCatalogueCommand(
						catalogueRequest = CatalogueRequest(
							catalogueRequestDTO.title,
							catalogueRequestDTO.parentId
						)
					)
				)

				call.respondToCommand(response)
			}

			patch("{id}/changeTitle/{newTitle}") {
				val response = eventBus.send(
					ChangeCatalogueTitleCommand(
						catalogueId = call.getParameter("id"),
						newTitle = call.getParameter("newTitle")
					)
				)

				call.respondToCommand(response)
			}

			patch("{id}/changeParent/{parentId}") {
				val parent = eventBus.send(
					GetCatalogueByIdQuery(call.getParameter("parentId"))
				)

				if (parent.assertIsSuccessfulRetrieval().not())
					return@patch call.respond(HttpStatusCode.NotFound)

				val response = eventBus.send(
					ChangeCatalogueParentCommand(
						catalogueId = call.getParameter("id"),
						newParent = parent.payload.get() as Catalogue
					)
				)

				call.respondToCommand(response)
			}

			patch("{id}/switchChildren/{firstChildId}/{secondChildId}") {
				val firstChild = eventBus.send(
					GetCatalogueByIdQuery(call.getParameter("firstChildId"))
				)

				val secondChild = eventBus.send(
					GetCatalogueByIdQuery(call.getParameter("secondChildId"))
				)

				if (firstChild.assertIsSuccessfulRetrieval().not()
					|| secondChild.assertIsSuccessfulRetrieval().not())
					return@patch call.respond(HttpStatusCode.NotFound)

				val response = eventBus.send(
					SwitchChildCataloguesCommand(
						catalogueId = call.getParameter("id"),
						first = firstChild.payload.get() as Catalogue,
						second = secondChild.payload.get() as Catalogue
					)
				)

				call.respondToCommand(response)
			}

			patch("{id}/orphan") {
				val response = eventBus.send(
					OrphanCatalogueCommand(
						catalogueId = call.getParameter("id")
					)
				)

				call.respondToCommand(response)
			}

			delete("{id}") {
				val response = eventBus.send(
					DeleteCatalogueCommand(
						catalogueId = call.getParameter("id")
					)
				)
				call.respondToCommand(response)
			}
		}
	}
}

/**
 * Runs Koin and the Ktor server.
 */
fun main(args: Array<String>) {
	startKoin(
		listOf(
			fakeNitriteDbModule,
			nitritePersistenceModule,
			nitriteEntryLinkerModule,
			simpleCqrsModule,
			exceptionLoggingModule,
			workflowModule
		)
	)

	embeddedServer(
		factory = Netty,
		environment = commandLineEnvironment(args)
	).start(true)
}

//region DTOs
//TODO Create a DTO Builder that will handle the response of a command and build the complete object.

data class CatalogueRequestDTO(val title: String, val parentId: String? = null)
//endregion

//region Helpers
/**
 * Converts a [StatusCode] to an [HttpStatusCode] object.
 */
private fun StatusCode.toHttpStatus(): HttpStatusCode {
	return HttpStatusCode.fromValue(this.value)
}

/**
 * Asserts that a [CommandResponse] has been successful and has
 * a payload to offer.
 */
private fun CommandResponse.assertIsSuccessfulRetrieval(): Boolean {
	return this.statusCode.isSuccess() && this.payload.isPresent
}

/**
 * Responds to a [CommandResponse] by converting the [StatusCode] to [HttpStatusCode]
 * and returns its payload, if any is present.
 */
private suspend fun ApplicationCall.respondToCommand(commandResponse: CommandResponse) {
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
private fun ApplicationCall.getParameter(paramName: String): String {
	return this.parameters[paramName] ?: throw ParameterNotFoundException(paramName)
}
//endregion

//region Exceptions
class ParameterNotFoundException(val paramName: String) : Throwable()
//endregion