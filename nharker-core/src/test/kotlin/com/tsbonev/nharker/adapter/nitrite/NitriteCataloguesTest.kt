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
import com.tsbonev.nharker.core.SortBy
import com.tsbonev.nharker.core.helpers.StubClock
import org.dizitart.kno2.filters.eq
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

    private val firstPresavedChildcatalogue = Catalogue(
            "::catalogue-id-1::",
            "::catalogue-title-1::",
            date,
            parentId = "::catalogue-id::"
    )

    private val secondPresavedChildcatalogue = Catalogue(
            "::catalogue-id-2::",
            "::catalogue-title-2::",
            date,
            parentId = "::catalogue-id::"
    )

    private val childCatalogue = Catalogue(
            "::catalogue-id-3::",
            "::catalogue-title-3::",
            date
    )

    private val catalogueRequest = CatalogueRequest(
            "::catalogue-title::"
    )

    private val catalogue = Catalogue(
            "::catalogue-id::",
            "::catalogue-title::",
            date,
            OrderedReferenceMap(
                    linkedMapOf(firstPresavedChildcatalogue.id to 0,
                            secondPresavedChildcatalogue.id to 1))
    )

    private val presavedCatalogue: Catalogue
        get() = db.getRepository(collectionName, Catalogue::class.java)
                .find(Catalogue::id eq catalogue.id).first()

    private val catalogues = NitriteCatalogues(
            db,
            collectionName,
            stubClock
    )

    @Before
    fun setUp() {
        db.getRepository(collectionName, Catalogue::class.java).insert(catalogue)
        db.getRepository(collectionName, Catalogue::class.java).insert(childCatalogue)
        db.getRepository(collectionName, Catalogue::class.java).insert(firstPresavedChildcatalogue)
        db.getRepository(collectionName, Catalogue::class.java).insert(secondPresavedChildcatalogue)
    }

    @Test
    fun `Create and return catalogue`() {
        db.getRepository(collectionName, Catalogue::class.java).remove(Catalogue::id eq catalogue.id)

        assertThat(catalogues.create(catalogueRequest).copy(id = catalogue.id),
                Is(catalogue.copy(childrenIds = OrderedReferenceMap())))
    }

    @Test(expected = CatalogueTitleTakenException::class)
    fun `Creating a catalogue with a taken title throws exception`() {
        catalogues.create(catalogueRequest)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Creating a catalogue with a non-existent parent throws exception`() {
        catalogues.create(CatalogueRequest("::new-catalogue::", "::non-existent-parent-id::"))
    }

    @Test
    fun `Save and return catalogue`() {
        db.getRepository(collectionName, Catalogue::class.java).remove(Catalogue::id eq catalogue.id)

        assertThat(catalogues.save(catalogue), Is(catalogue))
    }

    @Test
    fun `Retrieve catalogue by id`() {
        assertThat(catalogues.getById(catalogue.id).isPresent, Is(true))
        assertThat(catalogues.getById(catalogue.id).get(), Is(catalogue))
    }

    @Test
    fun `Retrieving non-existent catalogue returns empty`() {
        assertThat(catalogues.getById("::fake-catalogue-id::").isPresent, Is(false))
    }

    @Test
    fun `Retrieve all articles`() {
        assertThat(catalogues.getAll(SortBy.DESCENDING),
                Is(listOf(presavedCatalogue,
                        childCatalogue,
                        firstPresavedChildcatalogue,
                        secondPresavedChildcatalogue
                ).sortedBy { it.creationDate }))
    }

    @Test
    fun `Retrieve all articles, paginated`() {
        assertThat(catalogues.getAll(SortBy.ASCENDING, 1, 1), Is(listOf(presavedCatalogue)))
        assertThat(catalogues.getAll(SortBy.ASCENDING, 3, 4), Is(emptyList()))
    }

    @Test
    fun `Change catalogue title`() {
        val updatedCatalogue = catalogues.changeTitle(catalogue.id, "::new-title::")

        assertThat(updatedCatalogue, Is(catalogue.copy(title = updatedCatalogue.title)))
        assertThat(presavedCatalogue, Is(updatedCatalogue))
    }

    @Test(expected = CatalogueTitleTakenException::class)
    fun `Changing catalogue title to taken one throws exception`() {
        catalogues.changeTitle(catalogue.id, catalogue.title)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Changing title of non-existent catalogue throws exception`() {
        catalogues.changeTitle("::fake-id::", catalogue.title)
    }

    @Test
    fun `Change parent of catalogue`() {
        val updatedChild = catalogues.changeParentCatalogue(childCatalogue.id, catalogue)

        assertThat(updatedChild, Is(childCatalogue.copy(
                parentId = catalogue.id)))
        assertThat(presavedCatalogue.childrenIds.contains(childCatalogue.id), Is(true))
    }

    @Test(expected = CatalogueAlreadyAChildException::class)
    fun `Changing parent of a child to the same parent throws exception`() {
        catalogues.changeParentCatalogue(firstPresavedChildcatalogue.id, catalogue)
    }

    @Test(expected = CatalogueCircularInheritanceException::class)
    fun `Changing parent of a parent to its child throws exception`() {
        catalogues.changeParentCatalogue(catalogue.id, firstPresavedChildcatalogue)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Changing parent of non-existent catalogue throws exception`() {
        catalogues.changeParentCatalogue("::fake-parent-id::", childCatalogue)
    }

    @Test(expected = SelfContainedCatalogueException::class)
    fun `Changing parent of catalogue to the same value throws exception`() {
        catalogues.changeParentCatalogue(catalogue.id, catalogue)
    }

    @Test
    fun `Append catalogue to parent catalogue's children`() {
        val appendedChild = catalogues.appendChildCatalogue(catalogue.id, childCatalogue)

        assertThat(appendedChild, Is(childCatalogue.copy(parentId = catalogue.id)))
        assertThat(presavedCatalogue.childrenIds.raw(), Is(catalogue.childrenIds.raw().plus(
                childCatalogue.id to catalogue.childrenIds.raw().count()
        )))
    }

    @Test(expected = CatalogueCircularInheritanceException::class)
    fun `Appending parent to its own child throws exception`() {
        catalogues.appendChildCatalogue(firstPresavedChildcatalogue.id, catalogue)
    }

    @Test(expected = CatalogueAlreadyAChildException::class)
    fun `Appending catalogue that is already a child catalogue throws exception`() {
        catalogues.appendChildCatalogue(catalogue.id, firstPresavedChildcatalogue)
    }

    @Test(expected = SelfContainedCatalogueException::class)
    fun `Appending catalogue to itself throws exception`() {
        catalogues.appendChildCatalogue(catalogue.id, catalogue)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Appending child catalogue to non-existent catalogue throws exception`() {
        catalogues.appendChildCatalogue("::fake-catalogue-id::", catalogue)
    }

    @Test
    fun `Remove child catalogue from catalogue`() {
        val removedCatalogue = catalogues.removeChildCatalogue(catalogue.id, secondPresavedChildcatalogue)

        assertThat(removedCatalogue, Is(secondPresavedChildcatalogue.copy(parentId = null)))
        assertThat(presavedCatalogue.childrenIds.raw(), Is(mapOf(firstPresavedChildcatalogue.id to 0)))
    }

    @Test
    fun `Reorder child catalogues after deletion`() {
        catalogues.removeChildCatalogue(catalogue.id, firstPresavedChildcatalogue)

        assertThat(presavedCatalogue.childrenIds.raw(), Is(mapOf(secondPresavedChildcatalogue.id to 0)))
    }

    @Test(expected = CatalogueNotAChildException::class)
    fun `Removing child catalogue from non-parent throws exception`() {
        catalogues.removeChildCatalogue("::fake-parent-catalogue-id::", catalogue)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Removing child catalogue from non-existent parent throws exception`() {
        db.getRepository(collectionName, Catalogue::class.java).remove(Catalogue::id eq catalogue.id)
        catalogues.removeChildCatalogue(catalogue.id, firstPresavedChildcatalogue)
    }

    @Test
    fun `Switch child catalogues in catalogue`() {
        val updatedCatalogue = catalogues.switchChildCatalogues(catalogue.id,
                firstPresavedChildcatalogue, secondPresavedChildcatalogue)

        assertThat(updatedCatalogue.childrenIds.raw(), Is(mapOf(
                secondPresavedChildcatalogue.id to 0,
                firstPresavedChildcatalogue.id to 1
        )))
    }

    @Test(expected = CatalogueNotAChildException::class)
    fun `Switching child catalogues in non-containing catalogue throws exception`() {
        catalogues.switchChildCatalogues(catalogue.id, childCatalogue, secondPresavedChildcatalogue)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Switching child catalogues in non-existing catalogue throws exception`() {
        catalogues.switchChildCatalogues("::fake-catalogue-id::",
                firstPresavedChildcatalogue, secondPresavedChildcatalogue)
    }

    @Test
    fun `Delete and return catalogue`() {
        val savedCatalogue = presavedCatalogue

        val deletedCatalogue = catalogues.delete(savedCatalogue.id)

        assertThat(savedCatalogue, Is(deletedCatalogue))
        assertThat(
                db.getRepository(collectionName, Catalogue::class.java)
                        .find(Catalogue::id eq catalogue.id).firstOrNull(),
                Is(nullValue()))
    }

    @Test
    fun `Deleting catalogue with child catalogues folds their hierarchy ot its parent`() {
        val deletedCatalogue = catalogues.delete(presavedCatalogue.id)

        val childCatalogue = db.getRepository(collectionName, Catalogue::class.java)
                .find(Catalogue::id eq deletedCatalogue.childrenIds.raw().keys.first()).first()

        assertThat(childCatalogue.parentId, Is(deletedCatalogue.parentId))
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Deleting non-existent catalogue throws exception`() {
        catalogues.delete("::fake-catalogue-id::")
    }
}
