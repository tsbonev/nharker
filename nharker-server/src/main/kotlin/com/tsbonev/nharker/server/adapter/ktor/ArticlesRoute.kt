package com.tsbonev.nharker.server.adapter.ktor

import com.tsbonev.nharker.core.ArticleRequest
import com.tsbonev.nharker.core.CatalogueReference
import com.tsbonev.nharker.core.SortBy
import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.server.workflow.AddArticleToCatalogueCommand
import com.tsbonev.nharker.server.workflow.AppendEntryToArticleCommand
import com.tsbonev.nharker.server.workflow.AttachPropertyToArticleCommand
import com.tsbonev.nharker.server.workflow.CreateArticleCommand
import com.tsbonev.nharker.server.workflow.DeleteArticleCommand
import com.tsbonev.nharker.server.workflow.DetachPropertyFromArticleCommand
import com.tsbonev.nharker.server.workflow.GetAllArticlesQuery
import com.tsbonev.nharker.server.workflow.GetArticleByIdQuery
import com.tsbonev.nharker.server.workflow.GetCatalogueByIdQuery
import com.tsbonev.nharker.server.workflow.GetEntryByIdQuery
import com.tsbonev.nharker.server.workflow.GetPaginatedArticlesQuery
import com.tsbonev.nharker.server.workflow.RemoveArticleFromCatalogueCommand
import com.tsbonev.nharker.server.workflow.RemoveEntryFromArticleCommand
import com.tsbonev.nharker.server.workflow.RenameArticleCommand
import com.tsbonev.nharker.server.workflow.SearchArticleByTitleQuery
import com.tsbonev.nharker.server.workflow.SwitchEntriesInArticleCommand
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
fun Routing.articleRoute() {
	val eventBus: EventBus by inject()

	route("/article") {
		get("{articleId}") {
			val response = eventBus.send(
				GetArticleByIdQuery(
					articleId = call.getParameter("articleId")
				)
			)

			call.respondToCommand(response)
		}

		get("/all/{order}") {
			val response = eventBus.send(
				GetAllArticlesQuery(
					order = SortBy.valueOf(call.getParameter("order"))
				)
			)

			call.respondToCommand(response)
		}

		get("/paginated/{order}/{page}/{pageSize}") {
			val response = eventBus.send(
				GetPaginatedArticlesQuery(
					order = SortBy.valueOf(call.getParameter("order")),
					page = call.getParameter("page").toInt(),
					pageSize = call.getParameter("pageSize").toInt()
				)
			)

			call.respondToCommand(response)
		}

		get("/search/title/{searchString}") {
			val response = eventBus.send(
				SearchArticleByTitleQuery(
					searchString = call.getParameter("searchString")
				)
			)

			call.respondToCommand(response)
		}

		post {
			val articleRequestDto = call.receive<ArticleRequestDto>()

			val catalogues = mutableSetOf<CatalogueReference>()

			articleRequestDto.catalogueIds.forEach { catalogueId ->
				val response = eventBus.send(GetCatalogueByIdQuery(catalogueId))

				if (response.assertIsSuccessfulRetrieval().not())
					return@post call.respond(HttpStatusCode.NotFound)

				val catalogue = response.asCatalogue()

				catalogues.add(CatalogueReference(catalogue.id, catalogue.title))
			}

			val response = eventBus.send(
				CreateArticleCommand(
					articleRequest = ArticleRequest(
						fullTitle = articleRequestDto.title,
						catalogues = catalogues
					)
				)
			)

			call.respondToCommand(response)
		}

		patch("{articleId}/rename/{newTitle}") {
			val response = eventBus.send(
				RenameArticleCommand(
					articleId = call.getParameter("articleId"),
					newTitle = call.getParameter("newTitle")
				)
			)

			call.respondToCommand(response)
		}

		patch("{articleId}/entry/{entryId}/append") {
			val getEntryResponse = eventBus.send(
				GetEntryByIdQuery(
					entryId = call.getParameter("entryId")
				)
			)

			if (getEntryResponse.assertIsSuccessfulRetrieval().not())
				return@patch call.respond(HttpStatusCode.NotFound)

			val response = eventBus.send(
				AppendEntryToArticleCommand(
					entry = getEntryResponse.asEntry(),
					articleId = call.getParameter("articleId")
				)
			)

			call.respondToCommand(response)
		}

		patch("{articleId}/entry/{entryId}/remove") {
			val getEntryResponse = eventBus.send(
				GetEntryByIdQuery(
					entryId = call.getParameter("entryId")
				)
			)

			if (getEntryResponse.assertIsSuccessfulRetrieval().not())
				return@patch call.respond(HttpStatusCode.NotFound)

			val response = eventBus.send(
				RemoveEntryFromArticleCommand(
					entry = getEntryResponse.asEntry(),
					articleId = call.getParameter("articleId")
				)
			)

			call.respondToCommand(response)
		}

		patch("{articleId}/property/{propName}/attach") {
			val getEntryResponse = eventBus.send(
				GetEntryByIdQuery(
					entryId = call.getParameter("entryId")
				)
			)

			if (getEntryResponse.assertIsSuccessfulRetrieval().not())
				return@patch call.respond(HttpStatusCode.NotFound)

			val response = eventBus.send(
				AttachPropertyToArticleCommand(
					articleId = call.getParameter("articleId"),
					propName = call.getParameter("propName"),
					property = getEntryResponse.asEntry()
				)
			)

			call.respondToCommand(response)
		}

		patch("{articleId}/property/{propName}/detach") {
			val response = eventBus.send(
				DetachPropertyFromArticleCommand(
					articleId = call.getParameter("articleId"),
					propName = call.getParameter("propName")
				)
			)

			call.respondToCommand(response)
		}

		patch("{articleId}/entry/{firstEntryId}/switch/{secondEntryId}") {
			val getFirstEntryResponse = eventBus.send(
				GetEntryByIdQuery(call.getParameter("firstEntryId"))
			)

			val getSecondEntryResponse = eventBus.send(
				GetEntryByIdQuery(call.getParameter("firstEntryId"))
			)

			if (getFirstEntryResponse.assertIsSuccessfulRetrieval().not()
				|| getSecondEntryResponse.assertIsSuccessfulRetrieval().not())
				return@patch call.respond(HttpStatusCode.NotFound)

			val response = eventBus.send(
				SwitchEntriesInArticleCommand(
					call.getParameter("articleId"),
					getFirstEntryResponse.asEntry(),
					getSecondEntryResponse.asEntry()
				)
			)

			call.respondToCommand(response)
		}

		patch("{articleId}/catalogue/{catalogueId}/add") {
			val catalogueGetResponse = eventBus.send(
				GetCatalogueByIdQuery(
					catalogueId = call.getParameter("catalogueId")
				)
			)

			if (catalogueGetResponse.assertIsSuccessfulRetrieval().not())
				return@patch call.respond(HttpStatusCode.NotFound)

			val response = eventBus.send(
				AddArticleToCatalogueCommand(
					articleId = call.getParameter("articleId"),
					catalogue = catalogueGetResponse.asCatalogue()
				)
			)

			call.respondToCommand(response)
		}

		patch("{articleId}/catalogue/{catalogueId}/remove") {
			val catalogueGetResponse = eventBus.send(
				GetCatalogueByIdQuery(
					catalogueId = call.getParameter("catalogueId")
				)
			)

			if (catalogueGetResponse.assertIsSuccessfulRetrieval().not())
				return@patch call.respond(HttpStatusCode.NotFound)

			val response = eventBus.send(
				RemoveArticleFromCatalogueCommand(
					articleId = call.getParameter("articleId"),
					catalogue = catalogueGetResponse.asCatalogue()
				)
			)

			call.respondToCommand(response)
		}

		delete("{articleId}") {
			val response = eventBus.send(
				DeleteArticleCommand(
					articleId = call.getParameter("articleId")
				)
			)
			call.respondToCommand(response)
		}
	}
}

data class ArticleRequestDto(val title: String, val catalogueIds: Set<String>)