package com.tsbonev.nharker.server.end2end

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.core.CatalogueReference
import com.tsbonev.nharker.server.adapter.ktor.ArticleRequestDto
import com.tsbonev.nharker.server.adapter.ktor.CatalogueRequestDto
import com.tsbonev.nharker.server.main
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class ArticlesEndpointsTest {
	@Rule
	@JvmField
	val koinRule = KoinRule()

	private val root = "/article"
	private val catalogues = "/catalogue"

	@Test
	fun `Creates and retrieves articles`() = withTestApplication(Application::main) {
		val articlesAndCatalogues = setupTwoArticlesAndCatalogues()

		val firstArticle = articlesAndCatalogues.firstArticle
		val secondArticle = articlesAndCatalogues.secondArticle
		val parentCatalogue = articlesAndCatalogues.parentCatalogue
		val childCatalogue = articlesAndCatalogues.childCatalogue

		val savedFirstArticle = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${firstArticle.id}"
		}.asArticle()

		val savedSecondArticle = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${secondArticle.id}"
		}.asArticle()

		assertThat(savedFirstArticle, Is(firstArticle))
		assertThat(
			savedFirstArticle.catalogues, containsInAnyOrder(
				CatalogueReference(
					parentCatalogue.id,
					parentCatalogue.title
				),
				CatalogueReference(
					childCatalogue.id,
					childCatalogue.title
				)
			)
		)

		assertThat(savedSecondArticle, Is(secondArticle))
		assertThat(
			savedSecondArticle.catalogues, containsInAnyOrder(
				CatalogueReference(
					childCatalogue.id,
					childCatalogue.title
				)
			)
		)
	}

	@Test
	fun `Retrieves articles by parts of titles`() = withTestApplication(Application::main) {
		val articlesAndCatalogues = setupTwoArticlesAndCatalogues()

		val firstArticle = articlesAndCatalogues.firstArticle
		val secondArticle = articlesAndCatalogues.secondArticle

		val articlesWithArticleInTheTitle = handleRequest {
			method = HttpMethod.Get
			uri = "$root/search/title/article"
		}.asArticleList()

		val articlesWithMainInTheTitle = handleRequest {
			method = HttpMethod.Get
			uri = "$root/search/title/main"
		}.asArticleList()

		assertThat(
			articlesWithArticleInTheTitle, containsInAnyOrder(
				firstArticle, secondArticle
			)
		)
		assertThat(
			articlesWithMainInTheTitle, containsInAnyOrder(
				firstArticle
			)
		)
	}

	@Test
	fun `Changes the title of an article`() = withTestApplication(Application::main) {
		val articlesAndCatalogues = setupTwoArticlesAndCatalogues()

		val article = articlesAndCatalogues.firstArticle

		val newFirstArticleTitle = "::new-title::"

		val renamedArticle = handleRequest {
			method = HttpMethod.Patch
			uri = "$root/${article.id}/rename/$newFirstArticleTitle"
		}.asArticle()

		val retrievedArticle = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${article.id}"
		}.asArticle()

		assertThat(retrievedArticle.title, Is(newFirstArticleTitle))
		assertThat(retrievedArticle, Is(renamedArticle))
	}

	@Test
	fun `Adds and removes catalogues from articles`() = withTestApplication(Application::main) {
		val articlesAndCatalogues = setupTwoArticlesAndCatalogues()

		val firstArticle = articlesAndCatalogues.firstArticle
		val secondArticle = articlesAndCatalogues.secondArticle
		val parentCatalogue = articlesAndCatalogues.parentCatalogue
		val childCatalogue = articlesAndCatalogues.childCatalogue

		val addedToParentCatalogueSecondArticle = handleRequest {
			method = HttpMethod.Patch
			uri = "$root/${secondArticle.id}/catalogue/${parentCatalogue.id}/add"
		}.asArticle()

		val removedFromChildCatalogueFirstArticle = handleRequest {
			method = HttpMethod.Patch
			uri = "$root/${firstArticle.id}/catalogue/${childCatalogue.id}/remove"
		}.asArticle()

		assertThat(
			removedFromChildCatalogueFirstArticle.catalogues, containsInAnyOrder(
				CatalogueReference(
					parentCatalogue.id,
					parentCatalogue.title
				)
			)
		)
		assertThat(
			addedToParentCatalogueSecondArticle.catalogues,
			containsInAnyOrder(
				CatalogueReference(
					parentCatalogue.id,
					parentCatalogue.title
				),
				CatalogueReference(
					childCatalogue.id,
					childCatalogue.title
				)
			)
		)

		val retrievedAfterCatalogueChangeFirstArticle = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${firstArticle.id}"
		}.asArticle()

		val retrievedAfterCatalogueChangeSecondArticle = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${secondArticle.id}"
		}.asArticle()

		assertThat(retrievedAfterCatalogueChangeFirstArticle, Is(removedFromChildCatalogueFirstArticle))
		assertThat(retrievedAfterCatalogueChangeSecondArticle, Is(addedToParentCatalogueSecondArticle))
	}


	@Test
	fun `Retrieving catalogues when creating article fails when not found`() = withTestApplication(Application::main) {
		val response = handleRequest {
			method = HttpMethod.Post
			uri = root
			setBody(ArticleRequestDto("New Article", setOf("Catalogue")).toJson())
		}.response

		assertThat(response.status(), Is(HttpStatusCode.NotFound))
	}

	@Test
	fun `Retrieving catalogues when adding to article fails when not found`() = withTestApplication(Application::main) {
		val response = handleRequest {
			method = HttpMethod.Patch
			uri = "$root/article-id/catalogue/catalogue-id/add"
		}.response

		assertThat(response.status(), Is(HttpStatusCode.NotFound))
	}

	@Test
	fun `Retrieving catalogues when removing from article fails when not found`() = withTestApplication(Application::main) {
		val response = handleRequest {
			method = HttpMethod.Patch
			uri = "$root/article-id/catalogue/catalogue-id/remove"
		}.response

		assertThat(response.status(), Is(HttpStatusCode.NotFound))
	}


	/**
	 * Happy path setup.
	 */
	private data class TwoArticlesAndCatalogues(
		val firstArticle: Article,
		val secondArticle: Article,
		val parentCatalogue: Catalogue,
		val childCatalogue: Catalogue
	)

	/**
	 * Sets up two articles and catalogue for testing purposes.
	 */
	private fun TestApplicationEngine.setupTwoArticlesAndCatalogues(): TwoArticlesAndCatalogues {
		/**
		 * Create two catalogues.
		 */
		val parentCatalogue = handleRequest(HttpMethod.Post, catalogues) {
			setBody(
				CatalogueRequestDto("Parent Catalogue").toJson()
			)
		}.asCatalogue()

		val childCatalogue = handleRequest(HttpMethod.Post, catalogues) {
			setBody(
				CatalogueRequestDto("Child Catalogue", parentCatalogue.id).toJson()
			)
		}.asCatalogue()

		/**
		 * Create two articles.
		 */
		val firstArticle = handleRequest(HttpMethod.Post, root) {
			setBody(
				ArticleRequestDto("First Article Main", setOf(parentCatalogue.id, childCatalogue.id)).toJson()
			)
		}.asArticle()

		val secondArticle = handleRequest(HttpMethod.Post, root) {
			setBody(
				ArticleRequestDto("Second Article Subsidiary", setOf(childCatalogue.id)).toJson()
			)
		}.asArticle()

		return TwoArticlesAndCatalogues(firstArticle, secondArticle, parentCatalogue, childCatalogue)
	}
}