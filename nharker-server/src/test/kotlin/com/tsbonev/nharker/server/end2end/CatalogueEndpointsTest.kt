package com.tsbonev.nharker.server.end2end

import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.server.adapter.ktor.CatalogueRequestDto
import com.tsbonev.nharker.server.main
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class CatalogueEndpointsTest {
	@Rule
	@JvmField
	val koinRule = KoinRule()

	private val root = "/catalogue"

	@Test
	fun `Saves and retrieves catalogues`() = withTestApplication(Application::main) {
		val fourCatalogues = setupFourCatalogues()

		val catalogue = fourCatalogues.rootCatalogue

		val retrievedCatalogue = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${catalogue.id}"
		}.asCatalogue()

		assertThat(catalogue, Is(retrievedCatalogue))
	}

	@Test
	fun `Saves catalogues with preset hierarchies`() = withTestApplication(Application::main) {
		val fourCatalogues = setupFourCatalogues()

		val rootCatalogue = fourCatalogues.rootCatalogue
		val parentCatalogue = fourCatalogues.parentCatalogue

		assertThat(rootCatalogue.children.contains(parentCatalogue.id), Is(true))
		assertThat(parentCatalogue.parentId, Is(rootCatalogue.id))
	}

	@Test
	fun `Changes catalogue hierarchy`() = withTestApplication(Application::main) {
		val fourCatalogues = setupFourCatalogues()

		val rootCatalogue = fourCatalogues.rootCatalogue
		val parentCatalogue = fourCatalogues.parentCatalogue
		val firstChild = fourCatalogues.firstChild
		val secondChild = fourCatalogues.secondChild

		handleRequest {
			method = HttpMethod.Patch
			uri = "$root/${firstChild.id}/changeParent/${rootCatalogue.id}"
		}

		handleRequest {
			method = HttpMethod.Patch
			uri = "$root/${secondChild.id}/changeParent/${rootCatalogue.id}"
		}

		val rootWithNewChildren = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${rootCatalogue.id}"
		}.asCatalogue()

		assertThat(rootWithNewChildren.children.contains(firstChild.id), Is(true))
		assertThat(rootWithNewChildren.children.contains(secondChild.id), Is(true))

		val retrievedParentWithoutChildren = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${parentCatalogue.id}"
		}.asCatalogue()

		assertThat(retrievedParentWithoutChildren.children.contains(firstChild.id), Is(false))
		assertThat(retrievedParentWithoutChildren.children.contains(secondChild.id), Is(false))

		val updatedFirstChild = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${firstChild.id}"
		}.asCatalogue()

		val updatedSecondChild = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${secondChild.id}"
		}.asCatalogue()

		assertThat(updatedFirstChild.parentId, Is(rootCatalogue.id))
		assertThat(updatedSecondChild.parentId, Is(rootCatalogue.id))
	}

	@Test
	fun `Switches the order of children in catalogue`() = withTestApplication(Application::main) {
		val fourCatalogues = setupFourCatalogues()

		val parentCatalogue = fourCatalogues.parentCatalogue
		val firstChild = fourCatalogues.firstChild
		val secondChild = fourCatalogues.secondChild

		val parentWithSwitchedChildren = handleRequest {
			method = HttpMethod.Patch
			uri = "$root/${parentCatalogue.id}/switchChildren/${firstChild.id}/${secondChild.id}"
		}.asCatalogue()

		parentCatalogue.children.switch(firstChild.id, secondChild.id)

		assertThat(parentCatalogue, Is(parentWithSwitchedChildren))
	}

	@Test
	fun `Changes the title of a catalogue`() = withTestApplication(Application::main) {
		val fourCatalogues = setupFourCatalogues()

		val catalogue = fourCatalogues.parentCatalogue

		val newTitle = "New title"

		val catalogueWithChangedTitle = handleRequest {
			method = HttpMethod.Patch
			uri = "$root/${catalogue.id}/changeTitle/$newTitle"
		}.asCatalogue()

		assertThat(catalogueWithChangedTitle, Is(catalogue.copy(title = newTitle)))
	}

	@Test
	fun `Orphans a catalogue`() = withTestApplication(Application::main) {
		val fourCatalogues = setupFourCatalogues()

		val parentCatalogue = fourCatalogues.parentCatalogue
		val childCatalogue = fourCatalogues.firstChild

		val orphanedChild = handleRequest {
			method = HttpMethod.Patch
			uri = "$root/${childCatalogue.id}/orphan"
		}.asCatalogue()

		val parentWithRemovedChild = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${parentCatalogue.id}"
		}.asCatalogue()

		assertThat(orphanedChild, Is(childCatalogue.copy(parentId = null)))
		assertThat(parentWithRemovedChild.children.contains(childCatalogue.id), Is(false))
	}

	@Test
	fun `Deletes catalogue`() = withTestApplication(Application::main) {
		val fourCatalogues = setupFourCatalogues()

		val catalogue = fourCatalogues.parentCatalogue

		val deletedCatalogue = handleRequest {
			method = HttpMethod.Delete
			uri = "$root/${catalogue.id}"
		}.asCatalogue()

		val retrieveDeletedResponseStatus = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${catalogue.id}"
		}.response.status()

		assertThat(catalogue, Is(deletedCatalogue))
		assertThat(retrieveDeletedResponseStatus, Is(HttpStatusCode.NotFound))
	}

	@Test
	fun `Restructures hierarchy after deletion`() = withTestApplication(Application::main) {
		val fourCatalogues = setupFourCatalogues()

		val rootCatalogue = fourCatalogues.rootCatalogue
		val parentCatalogue = fourCatalogues.parentCatalogue
		val firstChild = fourCatalogues.firstChild
		val secondChild = fourCatalogues.secondChild

		handleRequest {
			method = HttpMethod.Delete
			uri = "$root/${parentCatalogue.id}"
		}

		val retrievedFirstChild = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${firstChild.id}"
		}.asCatalogue()

		val retrievedSecondChild = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${secondChild.id}"
		}.asCatalogue()

		assertThat(retrievedFirstChild.parentId, Is(rootCatalogue.id))
		assertThat(retrievedSecondChild.parentId, Is(rootCatalogue.id))

		val retrievedRootWithNewChildren = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${rootCatalogue.id}"
		}.asCatalogue()

		assertThat(retrievedRootWithNewChildren.children.contains(firstChild.id), Is(true))
		assertThat(retrievedRootWithNewChildren.children.contains(secondChild.id), Is(true))
		assertThat(retrievedRootWithNewChildren.children.contains(parentCatalogue.id), Is(false))
	}

	@Test
	fun `Retrieving parent when changing parent fails`() = withTestApplication(Application::main) {
		val response = handleRequest {
			method = HttpMethod.Patch
			uri = "$root/child-id/changeParent/parent-id"
		}.response

		assertThat(response.status(), Is(HttpStatusCode.NotFound))
	}

	@Test
	fun `Retrieving children when switching fails`() = withTestApplication(Application::main) {
		val response = handleRequest {
			method = HttpMethod.Patch
			uri = "$root/parent-id/switchChildren/first-id/second-id"
		}.response

		assertThat(response.status(), Is(HttpStatusCode.NotFound))
	}


	/**
	 * Happy path setup methods.
	 */
	private data class FourCatalogues(
		val rootCatalogue: Catalogue,
		val parentCatalogue: Catalogue,
		val firstChild: Catalogue,
		val secondChild: Catalogue
	)

	/**
	 * Sets up four catalogues in a hierarchy of root to parent to two children.
	 */
	private fun TestApplicationEngine.setupFourCatalogues(): FourCatalogues {
		val rootCatalogue = handleRequest(HttpMethod.Post, root) {
			setBody(
				CatalogueRequestDto("Root").toJson()
			)
		}.asCatalogue()

		val parentCatalogue = handleRequest(HttpMethod.Post, root) {
			setBody(
				CatalogueRequestDto("Parent", rootCatalogue.id).toJson()
			)
		}.asCatalogue()

		val firstChild = handleRequest(HttpMethod.Post, root) {
			setBody(
				CatalogueRequestDto("Child 1", parentCatalogue.id).toJson()
			)
		}.asCatalogue()

		val secondChild = handleRequest(HttpMethod.Post, root) {
			setBody(
				CatalogueRequestDto("Child 2", parentCatalogue.id).toJson()
			)
		}.asCatalogue()

		val updatedParentCatalogue = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${parentCatalogue.id}"
		}.asCatalogue()

		val updatedRootCatalogue = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${rootCatalogue.id}"
		}.asCatalogue()

		return FourCatalogues(updatedRootCatalogue, updatedParentCatalogue, firstChild, secondChild)
	}
}