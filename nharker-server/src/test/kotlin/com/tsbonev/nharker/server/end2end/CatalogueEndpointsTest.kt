package com.tsbonev.nharker.server.end2end

import com.tsbonev.nharker.server.CatalogueRequestDTO
import com.tsbonev.nharker.server.main
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.junit.Assert.assertThat
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
	fun `Catalogue endpoints happy path`() = withTestApplication(Application::main) {
		/**
		 * Create four catalogues.
		 */
		val rootCatalogue = handleRequest(HttpMethod.Post, root) {
			setBody(
				CatalogueRequestDTO("Root").toJson()
			)
		}.asCatalogue()

		val parentCatalogue = handleRequest(HttpMethod.Post, root) {
			setBody(
				CatalogueRequestDTO("Parent", rootCatalogue.id).toJson()
			)
		}.asCatalogue()

		val firstChild = handleRequest(HttpMethod.Post, root) {
			setBody(
				CatalogueRequestDTO("Child 1").toJson()
			)
		}.asCatalogue()

		val secondChild = handleRequest(HttpMethod.Post, root) {
			setBody(
				CatalogueRequestDTO("Child 2").toJson()
			)
		}.asCatalogue()

		/**
		 * Assert that the root has been notified of parent beign his child.
		 */
		val rootNotifiedOfParent = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${rootCatalogue.id}"
		}.asCatalogue()

		assertThat(rootNotifiedOfParent.children.contains(parentCatalogue.id), Is(true))

		/**
		 * Change the parent of the two children.
		 */
		handleRequest {
			method = HttpMethod.Patch
			uri = "$root/${firstChild.id}/changeParent/${parentCatalogue.id}"
		}

		handleRequest {
			method = HttpMethod.Patch
			uri = "$root/${secondChild.id}/changeParent/${parentCatalogue.id}"
		}

		/**
		 * Retrieve the parent that now has two children.
		 */
		val retrievedParentCatalogue = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${parentCatalogue.id}"
		}.asCatalogue()

		with(parentCatalogue) {
			children.append(firstChild.id)
			children.append(secondChild.id)
		}

		assertThat(retrievedParentCatalogue, Is(parentCatalogue))

		/**
		 * Switch the order of the two children.
		 */
		val parentWithSwitchedChildren = handleRequest {
			method = HttpMethod.Patch
			uri = "$root/${parentCatalogue.id}/switchChildren/${firstChild.id}/${secondChild.id}"
		}.asCatalogue()

		parentCatalogue.children.switch(firstChild.id, secondChild.id)

		assertThat(parentCatalogue, Is(parentWithSwitchedChildren))

		/**
		 * Change the title of a catalogue.
		 */
		val newTitle = "New title"

		val parentWithChangedTitle = handleRequest {
			method = HttpMethod.Patch
			uri = "$root/${parentCatalogue.id}/changeTitle/$newTitle"
		}.asCatalogue()

		assertThat(parentWithChangedTitle, Is(parentWithSwitchedChildren.copy(title = newTitle)))

		/**
		 * Orphan one of the children.
		 */
		val orphanedChild = handleRequest {
			method = HttpMethod.Patch
			uri = "$root/${firstChild.id}/orphan"
		}.asCatalogue()

		val parentWithRemovedChild = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${parentCatalogue.id}"
		}.asCatalogue()

		assertThat(orphanedChild, Is(firstChild.copy(parentId = null)))

		parentWithChangedTitle.children.subtract(firstChild.id)

		assertThat(parentWithRemovedChild, Is(parentWithChangedTitle))

		/**
		 * Delete the middle parent.
		 */
		handleRequest {
			method = HttpMethod.Delete
			uri = "$root/${parentCatalogue.id}"
		}.asCatalogue()

		val rootWithoutParent = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${rootCatalogue.id}"
		}.asCatalogue()

		assertThat(rootWithoutParent.children.contains(parentCatalogue.id), Is(false))

		/**
		 * Check that the root is now the parent of the folded hierarchy.
		 */
		val retrievedSecondChild = handleRequest {
			method = HttpMethod.Get
			uri = "$root/${secondChild.id}"
		}.asCatalogue()

		rootCatalogue.children.append(secondChild.id)

		assertThat(retrievedSecondChild.parentId, Is(rootWithoutParent.id))
		assertThat(rootWithoutParent, Is(rootCatalogue))
	}

	@Test
	fun `Retrieving parent when changing parent fails`() = withTestApplication(Application::main) {
		with(handleRequest {
			method = HttpMethod.Patch
			uri = "$root/child-id/changeParent/parent-id"
		}){
			assertThat(response.status(), Is(HttpStatusCode.NotFound))
		}
	}

	@Test
	fun `Retrieving children when switching fails`() = withTestApplication(Application::main) {
		with(handleRequest {
			method = HttpMethod.Patch
			uri = "$root/parent-id/switchChildren/first-id/second-id"
		}){
			assertThat(response.status(), Is(HttpStatusCode.NotFound))
		}
	}
}