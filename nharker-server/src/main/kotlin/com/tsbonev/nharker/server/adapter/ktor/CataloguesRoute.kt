package com.tsbonev.nharker.server.adapter.ktor

import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.core.CatalogueRequest
import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.server.workflow.ChangeCatalogueParentCommand
import com.tsbonev.nharker.server.workflow.ChangeCatalogueTitleCommand
import com.tsbonev.nharker.server.workflow.CreateCatalogueCommand
import com.tsbonev.nharker.server.workflow.DeleteCatalogueCommand
import com.tsbonev.nharker.server.workflow.GetCatalogueByIdQuery
import com.tsbonev.nharker.server.workflow.OrphanCatalogueCommand
import com.tsbonev.nharker.server.workflow.SwitchChildCataloguesCommand
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.route
import org.koin.ktor.ext.inject

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
fun Routing.catalogueRoute() {
	val eventBus: EventBus by inject()

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
			val catalogueRequestDTO = call.receive<CatalogueRequestDto>()
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

data class CatalogueRequestDto(val title: String, val parentId: String? = null)