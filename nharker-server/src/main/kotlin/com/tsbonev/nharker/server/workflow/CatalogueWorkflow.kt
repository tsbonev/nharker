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
import com.tsbonev.nharker.cqrs.Command
import com.tsbonev.nharker.cqrs.CommandHandler
import com.tsbonev.nharker.cqrs.CommandResponse
import com.tsbonev.nharker.cqrs.Event
import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.cqrs.EventHandler
import com.tsbonev.nharker.cqrs.Query
import com.tsbonev.nharker.cqrs.QueryResponse
import com.tsbonev.nharker.cqrs.StatusCode
import com.tsbonev.nharker.cqrs.Workflow
import com.tsbonev.nharker.server.helpers.ExceptionLogger

/**
 * Provides the command handlers that affect catalogues directly
 * and the command handlers that query catalogues.
 *
 * Provides the event handlers that are concerned with the
 * state of catalogues and articles that are in catalogues.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class CatalogueWorkflow(
	private val eventBus: EventBus,
	private val catalogues: Catalogues,
	private val exceptionLogger: ExceptionLogger
) : Workflow {
	//region Command Handlers
	/**
	 * Creates a catalogue.
	 * @code 201
	 * @payload The created catalogue.
	 * @publishes CatalogueCreatedEvent
	 *
	 * If the catalogue requests a non-existing parent, logs the parent's id.
	 * @code 404
	 * @exception CatalogueNotFoundException
	 *
	 * If the catalogue title is taken, logs the title.
	 * @code 400
	 * @exception CatalogueTitleTakenException
	 */
	@CommandHandler
	fun createCatalogue(command: CreateCatalogueCommand): CommandResponse {
		return try {
			val createdCatalogue = catalogues.create(command.catalogueRequest)

			eventBus.publish(CatalogueCreatedEvent(createdCatalogue))
			CommandResponse(StatusCode.Created, createdCatalogue)
		} catch (e: CatalogueNotFoundException) {
			exceptionLogger.logException(e)
		} catch (e: CatalogueTitleTakenException) {
			exceptionLogger.logException(e)
		}
	}

	/**
	 * Deletes a catalogue.
	 * @code 200
	 * @payload The deleted catalogue.
	 * @publishes CatalogueDeletedEvent
	 *
	 * If catalogue is not found by id, logs id.
	 * @code 404
	 * @exception CatalogueNotFoundException
	 */
	@CommandHandler
	fun deleteCatalogue(command: DeleteCatalogueCommand): CommandResponse {
		return try {
			val deletedCatalogue = catalogues.delete(command.catalogueId)

			eventBus.publish(CatalogueDeletedEvent(deletedCatalogue))
			CommandResponse(StatusCode.OK, deletedCatalogue)
		} catch (e: CatalogueNotFoundException) {
			exceptionLogger.logException(e)
		}
	}

	/**
	 * Changes the title of a catalogue.
	 * @code 200
	 * @payload The updated catalogue.
	 * @publishes CatalogueUpdatedEvent
	 *
	 * If the catalogue is not found by id, logs id.
	 * @code 404
	 * @exception CatalogueNotFoundException
	 *
	 * If the catalogue title is taken, logs title.
	 * @code 400
	 * @exception CatalogueTitleTakenException
	 */
	@CommandHandler
	fun changeCatalogueTitle(command: ChangeCatalogueTitleCommand): CommandResponse {
		return try {
			val updatedCatalogue = catalogues.changeTitle(command.catalogueId, command.newTitle)

			eventBus.publish(CatalogueUpdatedEvent(updatedCatalogue))
			CommandResponse(StatusCode.OK, updatedCatalogue)
		} catch (e: CatalogueNotFoundException) {
			exceptionLogger.logException(e)
		} catch (e: CatalogueTitleTakenException) {
			exceptionLogger.logException(e)
		}
	}

	/**
	 * Changes the parent of a catalogue.
	 * @code 200
	 * @payload The updated catalogue.
	 * @publishes CatalogueUpdatedEvent
	 *
	 * If the catalogue is not found by id, logs the id.
	 * @code 404
	 * @exception CatalogueNotFoundException
	 *
	 * If the catalogue is already a child, logs the catalogue id and the parent id.
	 * @code 400
	 * @exception CatalogueAlreadyAChildException
	 *
	 * If the parent catalogue is a child of the requested parent catalogue, logs the parent's and the child's ids.
	 * @code 400
	 * @exception CatalogueCircularInheritanceException
	 *
	 * If the catalogue is requested to become its own child, logs the catalogue's id.
	 * @code 400
	 * @exception SelfContainedCatalogueException
	 */
	@CommandHandler
	fun changeCatalogueParent(command: ChangeCatalogueParentCommand): CommandResponse {
		return try {
			val updatedCatalogue = catalogues.changeParentCatalogue(command.catalogueId, command.newParent)

			eventBus.publish(CatalogueUpdatedEvent(updatedCatalogue))
			CommandResponse(StatusCode.OK, updatedCatalogue)
		} catch (e: CatalogueNotFoundException) {
			exceptionLogger.logException(e)
		} catch (e: CatalogueAlreadyAChildException) {
			exceptionLogger.logException(e)
		} catch (e: CatalogueCircularInheritanceException) {
			exceptionLogger.logException(e)
		} catch (e: SelfContainedCatalogueException) {
			exceptionLogger.logException(e)
		}
	}

	/**
	 * Switches the order of two child catalogues.
	 * @code 200
	 * @payload The updated catalogue.
	 * @publishes CatalogueUpdatedEvent
	 *
	 * If the catalogue is not found by id, logs the id
	 * @code 404
	 * @exception CatalogueNotFoundException
	 *
	 * If the catalogue does not contain both child catalogues, logs the catalogue's id and the child catalogues' ids.
	 * @code 400
	 * @exception CatalogueNotAChildException
	 */
	@CommandHandler
	fun switchChildCatalogues(command: SwitchChildCataloguesCommand): CommandResponse {
		return try {
			val updatedCatalogue = catalogues.switchChildCatalogues(
				command.catalogueId, command.first, command.second
			)

			eventBus.publish(CatalogueUpdatedEvent(updatedCatalogue))
			CommandResponse(StatusCode.OK, updatedCatalogue)
		} catch (e: CatalogueNotFoundException) {
			exceptionLogger.logException(e)
		} catch (e: CatalogueNotAChildException) {
			exceptionLogger.logException(e)
		}
	}

	/**
	 * Orphans a catalogue.
	 * @code 200
	 * @payload The orphaned catalogue.
	 * @publishes CatalogueUpdatedEvent
	 *
	 * If the catalogue is not found by id, logs the id.
	 * @code 404
	 * @exception CatalogueNotFoundException
	 */
	@CommandHandler
	fun orphanCatalogue(command: OrphanCatalogueCommand): CommandResponse {
		return try {
			val updatedCatalogue = catalogues.orphanCatalogue(command.parentCatalogueId)

			eventBus.publish(CatalogueUpdatedEvent(updatedCatalogue))
			CommandResponse(StatusCode.OK, updatedCatalogue)
		} catch (e: CatalogueNotFoundException) {
			exceptionLogger.logException(e)
		}
	}

	/**
	 * Retrieves a catalogue by id.
	 * @code 200
	 * @payload The retrieved catalogue.
	 *
	 * If no catalogue is found, logs id.
	 * @code 404
	 * @exception CatalogueNotFoundException
	 */
	@CommandHandler
	fun getCatalogueById(query: GetCatalogueByIdQuery): QueryResponse {
		val possibleCatalogue = catalogues.getById(query.catalogueId)

		return if (possibleCatalogue.isPresent) CommandResponse(StatusCode.OK, possibleCatalogue.get())
		else exceptionLogger.logException(CatalogueNotFoundException(query.catalogueId))
	}
	//endregion

	//region Event Handlers
	/**
	 * Saves a restored catalogue by inserting it back into
	 * the inheritance tree.
	 */
	@EventHandler
	fun onCatalogueRestored(event: EntityRestoredEvent) {
		if (event.entityClass == Catalogue::class.java && event.entity is Catalogue) {
			event.entity
				.saveAsChildlessOrphan()
				.restoreToOldParent()
				.reappendChildren()
		}
	}
	//endregion

	/**
	 * Save the catalogue without any parent or children so it can
	 * be accessed by the others.
	 */
	private fun Catalogue.saveAsChildlessOrphan(): Catalogue {
		catalogues.save(
			this.copy(
				parentId = null,
				children = OrderedReferenceMap()
			)
		)

		return this
	}

	/**
	 * Checks if a catalogue's parent is still in existence.
	 */
	private fun Catalogue.restoreToOldParent(): Catalogue {
		val parentId = this.parentId

		return if (parentId != null) {
			val parentCatalogue = catalogues.getById(parentId)

			return if (parentCatalogue.isPresent) {
				catalogues.changeParentCatalogue(this.id, parentCatalogue.get())
			} else this.copy(parentId = null)
		} else this
	}

	/**
	 * Reappends the children that a catalogue had before deletion.
	 */
	private fun Catalogue.reappendChildren(): Catalogue {
		val reappendedCatalogue = this.copy(
			children = OrderedReferenceMap()
		)

		val existingChildren = this.extractExistingChildren()

		existingChildren
			.forEach { child, _ ->
				catalogues.changeParentCatalogue(child.id, this)
				reappendedCatalogue.children.append(child.id)
			}

		return reappendedCatalogue
	}

	/**
	 * Extract the existing children
	 * of a catalogue and sorts them by their original order.
	 */
	private fun Catalogue.extractExistingChildren(): Map<Catalogue, Int>{
		val existingChildren = hashMapOf<Catalogue, Int>()

		this.children.raw()
			.forEach { id, order ->
				val possibleChild = catalogues.getById(id)

				if (possibleChild.isPresent)
					existingChildren[possibleChild.get()] = order
			}

		return existingChildren.toList()
			.sortedBy { it.second }
			.toMap()
	}
}

//region Queries
data class GetCatalogueByIdQuery(val catalogueId: String) : Query
//endregion

//region Commands
data class CreateCatalogueCommand(val catalogueRequest: CatalogueRequest) : Command

data class CatalogueCreatedEvent(val catalogue: Catalogue) : Event

data class DeleteCatalogueCommand(val catalogueId: String) : Command
data class CatalogueDeletedEvent(val catalogue: Catalogue) : Event

data class ChangeCatalogueTitleCommand(val catalogueId: String, val newTitle: String) : Command
data class ChangeCatalogueParentCommand(val catalogueId: String, val newParent: Catalogue) : Command
data class OrphanCatalogueCommand(val parentCatalogueId: String) : Command
data class SwitchChildCataloguesCommand(val catalogueId: String, val first: Catalogue, val second: Catalogue) : Command
data class CatalogueUpdatedEvent(val catalogue: Catalogue) : Event
//endregion