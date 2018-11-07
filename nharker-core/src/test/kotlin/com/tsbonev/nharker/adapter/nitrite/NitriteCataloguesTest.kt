package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.core.CatalogueAlreadyAChildException
import com.tsbonev.nharker.core.CatalogueCircularInheritanceException
import com.tsbonev.nharker.core.CatalogueNotAChildException
import com.tsbonev.nharker.core.CatalogueNotFoundException
import com.tsbonev.nharker.core.CatalogueRequest
import com.tsbonev.nharker.core.CatalogueTitleTakenException
import com.tsbonev.nharker.core.OrderedReferenceMap
import com.tsbonev.nharker.core.SelfContainedCatalogueException
import com.tsbonev.nharker.core.helpers.StubClock
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.getRepository
import org.dizitart.kno2.nitrite
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteCataloguesTest {
	private val db = nitrite { }

	private val date = LocalDateTime.ofInstant(Instant.ofEpochSecond(1), ZoneOffset.UTC)
	private val stubClock = StubClock()
	private val collectionName = "TestCatalogues"

	private val firstPresavedChildCatalogue = Catalogue(
		"::catalogue-id-1::",
		"::catalogue-title-1::",
		date,
		parentId = "::catalogue-id::"
	)

	private val secondPresavedChildCatalogue = Catalogue(
		"::catalogue-id-2::",
		"::catalogue-title-2::",
		date,
		parentId = "::catalogue-id::"
	)

	private val childCatalogue = Catalogue(
		"::catalogue-id-3::",
		"::catalogue-title-3::",
		date,
		parentId = "::catalogue-id-4::"
	)

	private val parentCatalogue = Catalogue(
		"::catalogue-id-4::",
		"::catalogue-title-4::",
		date,
		children = OrderedReferenceMap(linkedMapOf(childCatalogue.id to 0))
	)

	private val catalogueRequest = CatalogueRequest(
		"::catalogue-title::"
	)

	private val catalogue = Catalogue(
		"::catalogue-id::",
		"::catalogue-title::",
		date,
		OrderedReferenceMap(
			linkedMapOf(
				firstPresavedChildCatalogue.id to 0,
				secondPresavedChildCatalogue.id to 1
			)
		)
	)

	private val presavedCatalogue: Catalogue
		get() = db.getRepository(collectionName, Catalogue::class.java)
			.find(Catalogue::id eq catalogue.id).first()

	private val presavedParentCatalogue: Catalogue
		get() = db.getRepository(collectionName, Catalogue::class.java)
			.find(Catalogue::id eq parentCatalogue.id).first()

	private val catalogues = NitriteCatalogues(
		db,
		collectionName,
		stubClock
	)

	@Before
	fun setUp() {
		db.getRepository(collectionName, Catalogue::class.java).insert(catalogue)
		db.getRepository(collectionName, Catalogue::class.java).insert(parentCatalogue)
		db.getRepository(collectionName, Catalogue::class.java).insert(childCatalogue)
		db.getRepository(collectionName, Catalogue::class.java).insert(firstPresavedChildCatalogue)
		db.getRepository(collectionName, Catalogue::class.java).insert(secondPresavedChildCatalogue)
	}

	@Test
	fun `Creating catalogue returns it`() {
		db.getRepository(collectionName, Catalogue::class.java).remove(Catalogue::id eq catalogue.id)

		assertThat(
			catalogues.create(catalogueRequest).copy(id = catalogue.id),
			Is(catalogue.copy(children = OrderedReferenceMap()))
		)
	}

	@Test(expected = CatalogueTitleTakenException::class)
	fun `Creating a catalogue with a taken title throws exception`() {
		catalogues.create(catalogueRequest)
	}

	@Test(expected = CatalogueNotFoundException::class)
	fun `Creating a catalogue with a non-existing parent throws exception`() {
		catalogues.create(CatalogueRequest("::new-catalogue::", "::non-existing-parent-id::"))
	}

	@Test
	fun `Saving catalogue returns it`() {
		db.getRepository(collectionName, Catalogue::class.java).remove(Catalogue::id eq catalogue.id)

		val savedCatalogue = catalogues.save(catalogue)

		assertThat(savedCatalogue, Is(presavedCatalogue))
	}

	@Test
	fun `Retrieves catalogue by id`() {
		assertThat(catalogues.getById(catalogue.id).isPresent, Is(true))
		assertThat(catalogues.getById(catalogue.id).get(), Is(catalogue))
	}

	@Test
	fun `Retrieving non-existing catalogue returns empty`() {
		assertThat(catalogues.getById("::fake-catalogue-id::").isPresent, Is(false))
	}

	@Test
	fun `Changes catalogue title`() {
		val updatedCatalogue = catalogues.changeTitle(catalogue.id, "::new-title::")

		assertThat(updatedCatalogue, Is(catalogue.copy(title = updatedCatalogue.title)))
		assertThat(presavedCatalogue, Is(updatedCatalogue))
	}

	@Test(expected = CatalogueTitleTakenException::class)
	fun `Changing catalogue title to taken one throws exception`() {
		catalogues.changeTitle(catalogue.id, catalogue.title)
	}

	@Test(expected = CatalogueNotFoundException::class)
	fun `Changing title of non-existing catalogue throws exception`() {
		catalogues.changeTitle("::fake-id::", catalogue.title)
	}

	@Test
	fun `Changes parent of catalogue`() {
		val updatedChild = catalogues.changeParentCatalogue(childCatalogue.id, catalogue)

		assertThat(
			updatedChild, Is(
				childCatalogue.copy(
					parentId = catalogue.id
				)
			)
		)
		assertThat(presavedCatalogue.children.contains(childCatalogue.id), Is(true))
	}

	@Test
	fun `Changing parent of catalogue updates old parent`() {
		catalogues.changeParentCatalogue(childCatalogue.id, catalogue)

		parentCatalogue.children.subtract(childCatalogue.id)

		assertThat(presavedParentCatalogue, Is(parentCatalogue))
	}

	@Test(expected = CatalogueAlreadyAChildException::class)
	fun `Changing parent of a child to the same parent throws exception`() {
		catalogues.changeParentCatalogue(firstPresavedChildCatalogue.id, catalogue)
	}

	@Test(expected = CatalogueCircularInheritanceException::class)
	fun `Changing parent of a parent to its child throws exception`() {
		catalogues.changeParentCatalogue(catalogue.id, firstPresavedChildCatalogue)
	}

	@Test(expected = CatalogueNotFoundException::class)
	fun `Changing parent of non-existing catalogue throws exception`() {
		catalogues.changeParentCatalogue("::fake-parent-id::", childCatalogue)
	}

	@Test(expected = SelfContainedCatalogueException::class)
	fun `Changing parent of catalogue to the same value throws exception`() {
		catalogues.changeParentCatalogue(catalogue.id, catalogue)
	}

	@Test
	fun `Orphans catalogue`() {
		val orphanedCatalogue = catalogues.orphanCatalogue(childCatalogue.id)

		assertThat(orphanedCatalogue, Is(childCatalogue.copy(parentId = null)))

		parentCatalogue.children.subtract(childCatalogue.id)

		assertThat(presavedParentCatalogue, Is(parentCatalogue))
	}

	@Test(expected = CatalogueNotFoundException::class)
	fun `Orphaning non-existing catalogue throws exception`() {
		db.getRepository(collectionName, Catalogue::class.java).remove(Catalogue::id eq catalogue.id)
		catalogues.orphanCatalogue(catalogue.id)
	}

	@Test
	fun `Switches child catalogues' order in catalogue`() {
		val updatedCatalogue = catalogues.switchChildCatalogues(
			catalogue.id,
			firstPresavedChildCatalogue, secondPresavedChildCatalogue
		)

		assertThat(
			updatedCatalogue.children.raw(), Is(
				mapOf(
					secondPresavedChildCatalogue.id to 0,
					firstPresavedChildCatalogue.id to 1
				)
			)
		)
	}

	@Test(expected = CatalogueNotAChildException::class)
	fun `Switching child catalogues in non-containing catalogue throws exception`() {
		catalogues.switchChildCatalogues(catalogue.id, childCatalogue, secondPresavedChildCatalogue)
	}

	@Test(expected = CatalogueNotFoundException::class)
	fun `Switching child catalogues in non-existing catalogue throws exception`() {
		catalogues.switchChildCatalogues(
			"::fake-catalogue-id::",
			firstPresavedChildCatalogue, secondPresavedChildCatalogue
		)
	}

	@Test
	fun `Deleting catalogue returns it`() {
		val savedCatalogue = presavedCatalogue

		val deletedCatalogue = catalogues.delete(savedCatalogue.id)

		assertThat(savedCatalogue, Is(deletedCatalogue))
		assertThat(
			db.getRepository(collectionName, Catalogue::class.java)
				.find(Catalogue::id eq catalogue.id).firstOrNull(),
			Is(nullValue())
		)
	}

	@Test
	fun `Deleting catalogue with children folds their hierarchy to its parent when parent is an orphan`() {
		val deletedCatalogue = catalogues.delete(presavedCatalogue.id)

		val firstChildId = deletedCatalogue.children.raw().keys.first()

		val childCatalogue = db.getRepository(collectionName, Catalogue::class.java)
			.find(Catalogue::id eq firstChildId).first()

		assertThat(childCatalogue.parentId, Is(deletedCatalogue.parentId))
	}

	@Test
	fun `Deleting catalogue with children folds their hierarchy to its own parent`() {
		val childCatalogue = Catalogue("::child-id::", "Child title", date, parentId = "::parent-id::")
		val parentCatalogue = Catalogue(
			"::parent-id::", "Parent title", date,
			OrderedReferenceMap(linkedMapOf(childCatalogue.id to 0)),
			parentId = "::root-id::"
		)
		val rootCatalogue = Catalogue(
			"::root-id::", "Root title", date,
			OrderedReferenceMap(linkedMapOf(parentCatalogue.id to 0))
		)

		with(db.getRepository(collectionName, Catalogue::class.java)){
			insert(childCatalogue)
			insert(parentCatalogue)
			insert(rootCatalogue)
		}

		catalogues.delete(parentCatalogue.id)

		val retrievedRoot = db.getRepository(collectionName, Catalogue::class.java)
			.find(Catalogue::id eq rootCatalogue.id).first()

		val retrievedChild = db.getRepository(collectionName, Catalogue::class.java)
			.find(Catalogue::id eq childCatalogue.id).first()

		val retrievedParent = db.getRepository(collectionName, Catalogue::class.java)
			.find(Catalogue::id eq parentCatalogue.id).firstOrNull()

		assertThat(retrievedChild.parentId, Is(parentCatalogue.parentId))
		assertThat(retrievedRoot.children.contains(retrievedChild.id), Is(true))
		assertThat(retrievedParent, Is(nullValue()))
	}

	@Test(expected = CatalogueNotFoundException::class)
	fun `Deleting non-existing catalogue throws exception`() {
		catalogues.delete("::fake-catalogue-id::")
	}
}
