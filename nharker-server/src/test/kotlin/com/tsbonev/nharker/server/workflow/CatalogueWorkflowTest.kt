package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.core.CatalogueAlreadyAChildException
import com.tsbonev.nharker.core.CatalogueCircularInheritanceException
import com.tsbonev.nharker.core.CatalogueNotAChildException
import com.tsbonev.nharker.core.CatalogueNotFoundException
import com.tsbonev.nharker.core.CatalogueRequest
import com.tsbonev.nharker.core.CatalogueTitleTakenException
import com.tsbonev.nharker.core.Catalogues
import com.tsbonev.nharker.core.OrderedReferenceMap
import com.tsbonev.nharker.core.SelfContainedCatalogueException
import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.cqrs.StatusCode
import com.tsbonev.nharker.server.helpers.ExceptionLogger
import org.jmock.AbstractExpectations.returnValue
import org.jmock.AbstractExpectations.throwException
import org.jmock.Expectations
import org.jmock.Mockery
import org.jmock.integration.junit4.JUnitRuleMockery
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class CatalogueWorkflowTest {
	@Rule
	@JvmField
	val context: JUnitRuleMockery = JUnitRuleMockery()

	private val eventBus = context.mock(EventBus::class.java)
	private val catalogues = context.mock(Catalogues::class.java)

	private val exceptionLogger = ExceptionLogger()
	private val catalogueRequest = CatalogueRequest(
		"::catalogue-title::",
		parentId = "::parent-id::"
	)

	private val date = LocalDateTime.ofEpochSecond(1, 1, ZoneOffset.UTC)

	private val parentCatalogue = Catalogue(
		"::parent-catalogue-id::",
		"::parent-title::",
		date
	)

	private val childCatalogue = Catalogue(
		"::child-catalogue-id::",
		"::child-title::",
		date,
		parentId = parentCatalogue.id
	)

	private val restoredCatalogue = Catalogue(
		"::catalogue-id::",
		"::title::",
		date,
		parentId = "::parent-catalogue-id::",
		children = OrderedReferenceMap(linkedMapOf(childCatalogue.id to 0))
	)

	private val catalogue = Catalogue(
		"::id::",
		"::catalogue-title::",
		date
	)

	private val catalogueWorkflow = CatalogueWorkflow(eventBus, catalogues, exceptionLogger)

	@Test
	fun `Creating a catalogue returns it`() {
		context.expecting {
			oneOf(catalogues).create(catalogueRequest)
			will(returnValue(catalogue))

			oneOf(eventBus).publish(CatalogueCreatedEvent(catalogue))
		}

		val response = catalogueWorkflow.createCatalogue(CreateCatalogueCommand(catalogueRequest))

		assertThat(response.statusCode, Is(StatusCode.Created))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as Catalogue, Is(catalogue))
	}

	@Test
	fun `Creating a catalogue with a taken title returns bad request`() {
		context.expecting {
			oneOf(catalogues).create(catalogueRequest)
			will(throwException(CatalogueTitleTakenException(catalogueRequest.title)))
		}

		val response = catalogueWorkflow.createCatalogue(CreateCatalogueCommand(catalogueRequest))

		assertThat(response.statusCode, Is(StatusCode.BadRequest))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Creating a catalogue with a non-existing parent returns not found`() {
		context.expecting {
			oneOf(catalogues).create(catalogueRequest)
			will(throwException(CatalogueNotFoundException(catalogueRequest.parentId!!)))
		}

		val response = catalogueWorkflow.createCatalogue(CreateCatalogueCommand(catalogueRequest))

		assertThat(response.statusCode, Is(StatusCode.NotFound))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Retrieves catalogue by id`() {
		context.expecting {
			oneOf(catalogues).getById(catalogue.id)
			will(returnValue(Optional.of(catalogue)))
		}

		val response = catalogueWorkflow.getCatalogueById(GetCatalogueByIdQuery(catalogue.id))

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as Catalogue, Is(catalogue))
	}

	@Test
	fun `Retrieving non-existing catalogue returns not found`() {
		context.expecting {
			oneOf(catalogues).getById(catalogue.id)
			will(returnValue(Optional.empty<Catalogue>()))
		}

		val response = catalogueWorkflow.getCatalogueById(GetCatalogueByIdQuery(catalogue.id))

		assertThat(response.statusCode, Is(StatusCode.NotFound))
		assertThat(response.payload.isPresent, Is(false))
	}


	@Test
	fun `Deleting catalogue returns it`() {
		context.expecting {
			oneOf(catalogues).delete(catalogue.id)
			will(returnValue(catalogue))

			oneOf(eventBus).publish(CatalogueDeletedEvent(catalogue))
		}

		val response = catalogueWorkflow.deleteCatalogue(DeleteCatalogueCommand(catalogue.id))

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as Catalogue, Is(catalogue))
	}

	@Test
	fun `Deleting non-existing catalogue returns not found`() {
		context.expecting {
			oneOf(catalogues).delete(catalogue.id)
			will(throwException(CatalogueNotFoundException(catalogue.id)))
		}

		val response = catalogueWorkflow.deleteCatalogue(DeleteCatalogueCommand(catalogue.id))

		assertThat(response.statusCode, Is(StatusCode.NotFound))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Changes catalogue title`() {
		context.expecting {
			oneOf(catalogues).changeTitle(catalogue.id, "::new-title::")
			will(returnValue(catalogue))

			oneOf(eventBus).publish(CatalogueUpdatedEvent(catalogue))
		}

		val response = catalogueWorkflow.changeCatalogueTitle(
			ChangeCatalogueTitleCommand(catalogue.id, "::new-title::")
		)

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as Catalogue, Is(catalogue))
	}

	@Test
	fun `Changing catalogue title to a taken one returns bad request`() {
		val newTitle = "::new-title::"

		context.expecting {
			oneOf(catalogues).changeTitle(catalogue.id, newTitle)
			will(throwException(CatalogueTitleTakenException(newTitle)))
		}

		val response = catalogueWorkflow.changeCatalogueTitle(
			ChangeCatalogueTitleCommand(catalogue.id, newTitle)
		)

		assertThat(response.statusCode, Is(StatusCode.BadRequest))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Changing title of non-existing catalogue returns not found`() {
		val newTitle = "::new-title::"

		context.expecting {
			oneOf(catalogues).changeTitle(catalogue.id, newTitle)
			will(throwException(CatalogueNotFoundException(catalogue.id)))
		}

		val response = catalogueWorkflow.changeCatalogueTitle(
			ChangeCatalogueTitleCommand(catalogue.id, newTitle)
		)

		assertThat(response.statusCode, Is(StatusCode.NotFound))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Changes catalogue parent`() {
		context.expecting {
			oneOf(catalogues).changeParentCatalogue(catalogue.id, catalogue)
			will(returnValue(catalogue))

			oneOf(eventBus).publish(CatalogueUpdatedEvent(catalogue))
		}

		val response = catalogueWorkflow.changeCatalogueParent(
			ChangeCatalogueParentCommand(catalogue.id, catalogue)
		)

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as Catalogue, Is(catalogue))
	}

	@Test
	fun `Changing catalogue parent to the same parent returns bad request`() {
		context.expecting {
			oneOf(catalogues).changeParentCatalogue(catalogue.id, catalogue)
			will(throwException(CatalogueAlreadyAChildException(catalogue.id, catalogue.id)))
		}

		val response = catalogueWorkflow.changeCatalogueParent(
			ChangeCatalogueParentCommand(catalogue.id, catalogue)
		)

		assertThat(response.statusCode, Is(StatusCode.BadRequest))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Changing catalogue parent to itself returns bad request`() {
		context.expecting {
			oneOf(catalogues).changeParentCatalogue(catalogue.id, catalogue)
			will(throwException(SelfContainedCatalogueException(catalogue.id)))
		}

		val response = catalogueWorkflow.changeCatalogueParent(
			ChangeCatalogueParentCommand(catalogue.id, catalogue)
		)

		assertThat(response.statusCode, Is(StatusCode.BadRequest))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Changing catalogue parent to its child returns bad request`() {
		context.expecting {
			oneOf(catalogues).changeParentCatalogue(catalogue.id, catalogue)
			will(throwException(CatalogueCircularInheritanceException(catalogue.id, catalogue.id)))
		}

		val response = catalogueWorkflow.changeCatalogueParent(
			ChangeCatalogueParentCommand(catalogue.id, catalogue)
		)

		assertThat(response.statusCode, Is(StatusCode.BadRequest))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Changing parent of non-existing catalogue returns not found`() {
		context.expecting {
			oneOf(catalogues).changeParentCatalogue(catalogue.id, catalogue)
			will(throwException(CatalogueNotFoundException(catalogue.id)))
		}

		val response = catalogueWorkflow.changeCatalogueParent(
			ChangeCatalogueParentCommand(catalogue.id, catalogue)
		)

		assertThat(response.statusCode, Is(StatusCode.NotFound))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Switches child catalogue order in catalogue`() {
		context.expecting {
			oneOf(catalogues).switchChildCatalogues(catalogue.id, catalogue, catalogue)
			will(returnValue(catalogue))

			oneOf(eventBus).publish(CatalogueUpdatedEvent(catalogue))
		}

		val response = catalogueWorkflow.switchChildCatalogues(
			SwitchChildCataloguesCommand(catalogue.id, catalogue, catalogue)
		)

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as Catalogue, Is(catalogue))
	}

	@Test
	fun `Switching child catalogue orders in a catalogue that does not contain both returns bad request`() {
		context.expecting {
			oneOf(catalogues).switchChildCatalogues(catalogue.id, catalogue, catalogue)
			will(throwException(CatalogueNotAChildException(catalogue.id, catalogue.id)))
		}

		val response = catalogueWorkflow.switchChildCatalogues(
			SwitchChildCataloguesCommand(catalogue.id, catalogue, catalogue)
		)

		assertThat(response.statusCode, Is(StatusCode.BadRequest))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Switching child catalogue orders in a non-existing catalogue returns not found`() {
		context.expecting {
			oneOf(catalogues).switchChildCatalogues(catalogue.id, catalogue, catalogue)
			will(throwException(CatalogueNotFoundException(catalogue.id)))
		}

		val response = catalogueWorkflow.switchChildCatalogues(
			SwitchChildCataloguesCommand(catalogue.id, catalogue, catalogue)
		)

		assertThat(response.statusCode, Is(StatusCode.NotFound))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Removes child catalogue from catalogue`() {
		context.expecting {
			oneOf(catalogues).orphanCatalogue(catalogue.id)
			will(returnValue(catalogue))

			oneOf(eventBus).publish(CatalogueUpdatedEvent(catalogue))
		}

		val response = catalogueWorkflow.orphanCatalogue(
			OrphanCatalogueCommand(catalogue.id)
		)

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as Catalogue, Is(catalogue))
	}

	@Test
	fun `Removing child catalogue from a non-existing catalogue returns not found`() {
		context.expecting {
			oneOf(catalogues).orphanCatalogue(catalogue.id)
			will(throwException(CatalogueNotFoundException(catalogue.id)))
		}

		val response = catalogueWorkflow.orphanCatalogue(
			OrphanCatalogueCommand(catalogue.id)
		)

		assertThat(response.statusCode, Is(StatusCode.NotFound))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Saves orphan childless catalogue when restored`() {
		context.expecting {
			oneOf(catalogues).save(catalogue)
		}

		catalogueWorkflow.onCatalogueRestored(EntityRestoredEvent(catalogue, Catalogue::class.java))
	}

	@Test
	fun `Reattaches catalogue to old parent when restoring`() {
		context.expecting {
			oneOf(catalogues).save(restoredCatalogue.copy(parentId = null, children = OrderedReferenceMap()))

			oneOf(catalogues).getById(parentCatalogue.id)
			will(returnValue(Optional.of(parentCatalogue)))

			oneOf(catalogues).changeParentCatalogue(restoredCatalogue.id, parentCatalogue)
			will(returnValue(restoredCatalogue.copy(children = OrderedReferenceMap())))
		}

		catalogueWorkflow.onCatalogueRestored(
			EntityRestoredEvent(
				restoredCatalogue.copy(children = OrderedReferenceMap()),
				Catalogue::class.java
			)
		)
	}

	@Test
	fun `Skips reattaching to parent when not found`(){
		context.expecting {
			oneOf(catalogues).save(restoredCatalogue.copy(parentId = null, children = OrderedReferenceMap()))

			oneOf(catalogues).getById(parentCatalogue.id)
			will(returnValue(Optional.empty<Catalogue>()))

			never(catalogues).changeParentCatalogue(restoredCatalogue.id, parentCatalogue)
		}

		catalogueWorkflow.onCatalogueRestored(
			EntityRestoredEvent(
				restoredCatalogue.copy(children = OrderedReferenceMap()),
				Catalogue::class.java
			)
		)
	}

	@Test
	fun `Reappends children to catalogue when restoring`(){
		context.expecting {
			oneOf(catalogues).save(restoredCatalogue.copy(parentId = null, children = OrderedReferenceMap()))

			oneOf(catalogues).getById(childCatalogue.id)
			will(returnValue(Optional.of(childCatalogue)))

			oneOf(catalogues).changeParentCatalogue(childCatalogue.id, restoredCatalogue.copy(parentId = null))
		}

		catalogueWorkflow.onCatalogueRestored(
			EntityRestoredEvent(
				restoredCatalogue.copy(parentId = null),
				Catalogue::class.java
			)
		)
	}

	@Test
	fun `Skips non-existing children when restoring`(){
		context.expecting {
			oneOf(catalogues).save(restoredCatalogue.copy(parentId = null, children = OrderedReferenceMap()))

			oneOf(catalogues).getById(childCatalogue.id)
			will(returnValue(Optional.empty<Catalogue>()))

			never(catalogues).changeParentCatalogue(childCatalogue.id, restoredCatalogue.copy(parentId = null))
		}

		catalogueWorkflow.onCatalogueRestored(
			EntityRestoredEvent(
				restoredCatalogue.copy(parentId = null),
				Catalogue::class.java
			)
		)
	}

	@Test
	fun `Restores catalogue to inheritance tree fully`(){
		context.expecting {
			//Saves as childless orphan
			oneOf(catalogues).save(restoredCatalogue.copy(parentId = null, children = OrderedReferenceMap()))

			//Checks parent for existence
			oneOf(catalogues).getById(parentCatalogue.id)
			will(returnValue(Optional.of(parentCatalogue)))

			//Attaches to parent
			oneOf(catalogues).changeParentCatalogue(restoredCatalogue.id, parentCatalogue)
			will(returnValue(restoredCatalogue))

			//Checks children for existence
			oneOf(catalogues).getById(childCatalogue.id)
			will(returnValue(Optional.of(childCatalogue)))

			//Attaches children to parent
			oneOf(catalogues).changeParentCatalogue(childCatalogue.id, restoredCatalogue)
		}

		catalogueWorkflow.onCatalogueRestored(
			EntityRestoredEvent(
				restoredCatalogue,
				Catalogue::class.java
			)
		)
	}

	@Test
	fun `Ignores foreign restored entities`() {
		context.expecting {
			never(catalogues).save(catalogue)
		}

		catalogueWorkflow.onCatalogueRestored(EntityRestoredEvent(catalogue, String::class.java))
	}

	private fun Mockery.expecting(block: Expectations.() -> Unit) {
		checking(Expectations().apply(block))
	}
}