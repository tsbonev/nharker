package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.core.CatalogueAlreadyAChildException
import com.tsbonev.nharker.core.CatalogueCircularInheritanceException
import com.tsbonev.nharker.core.CatalogueNotAChildException
import com.tsbonev.nharker.core.CatalogueNotFoundException
import com.tsbonev.nharker.core.CatalogueRequest
import com.tsbonev.nharker.core.CatalogueTitleTakenException
import com.tsbonev.nharker.core.SelfContainedCatalogueException
import com.tsbonev.nharker.core.SortBy
import com.tsbonev.nharker.core.helpers.StubClock
import com.tsbonev.nharker.core.helpers.append
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

    private val firstPresavedSubcatalogue = Catalogue(
            "::catalogue-id-1::",
            "::catalogue-title-1::",
            date,
            parentId = "::catalogue-id::"
    )

    private val secondPresavedSubcatalogue = Catalogue(
            "::catalogue-id-2::",
            "::catalogue-title-2::",
            date,
            parentId = "::catalogue-id::"
    )

    private val subCatalogue = Catalogue(
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
            mapOf(firstPresavedSubcatalogue.id to 0,
                    secondPresavedSubcatalogue.id to 1)
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
        db.getRepository(collectionName, Catalogue::class.java).insert(subCatalogue)
        db.getRepository(collectionName, Catalogue::class.java).insert(firstPresavedSubcatalogue)
        db.getRepository(collectionName, Catalogue::class.java).insert(secondPresavedSubcatalogue)
    }

    @Test
    fun `Create and return catalogue`() {
        db.getRepository(collectionName, Catalogue::class.java).remove(Catalogue::id eq catalogue.id)

        assertThat(catalogues.create(catalogueRequest).copy(id = catalogue.id),
                Is(catalogue.copy(childrenIds = emptyMap())))
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
                        subCatalogue,
                        firstPresavedSubcatalogue,
                        secondPresavedSubcatalogue
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
        val updatedChild = catalogues.changeParentCatalogue(subCatalogue.id, catalogue)

        assertThat(updatedChild, Is(subCatalogue.copy(
                parentId = catalogue.id)))
        assertThat(presavedCatalogue, Is(catalogue.copy(
                childrenIds = catalogue.childrenIds.append(subCatalogue.id))))
    }

    @Test(expected = CatalogueAlreadyAChildException::class)
    fun `Changing parent of a child to the same parent throws exception`() {
        catalogues.changeParentCatalogue(firstPresavedSubcatalogue.id, catalogue)
    }

    @Test(expected = CatalogueCircularInheritanceException::class)
    fun `Changing parent of a parent to its child throws exception`() {
        catalogues.changeParentCatalogue(catalogue.id, firstPresavedSubcatalogue)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Changing parent of non-existent catalogue throws exception`() {
        catalogues.changeParentCatalogue("::fake-parent-id::", subCatalogue)
    }

    @Test(expected = SelfContainedCatalogueException::class)
    fun `Changing parent of catalogue to the same value throws exception`() {
        catalogues.changeParentCatalogue(catalogue.id, catalogue)
    }

    @Test
    fun `Append catalogue to catalogue subcatalogues`() {
        val appendedChild = catalogues.appendChildCatalogue(catalogue.id, subCatalogue)

        assertThat(appendedChild, Is(subCatalogue.copy(parentId = catalogue.id)))
        assertThat(presavedCatalogue, Is(catalogue.copy(childrenIds = catalogue.childrenIds.plus(
                subCatalogue.id to catalogue.childrenIds.count()
        ))))
    }

    @Test(expected = CatalogueCircularInheritanceException::class)
    fun `Appending parent to its own child throws exception`() {
        catalogues.appendChildCatalogue(firstPresavedSubcatalogue.id, catalogue)
    }

    @Test(expected = CatalogueAlreadyAChildException::class)
    fun `Appending catalogue that is already a subcatalogue throws exception`() {
        catalogues.appendChildCatalogue(catalogue.id, firstPresavedSubcatalogue)
    }

    @Test(expected = SelfContainedCatalogueException::class)
    fun `Appending catalogue to itself throws exception`() {
        catalogues.appendChildCatalogue(catalogue.id, catalogue)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Appending subcatalogue to non-existent catalogue throws exception`() {
        catalogues.appendChildCatalogue("::fake-catalogue-id::", catalogue)
    }

    @Test
    fun `Remove subcatalogue from catalogue`() {
        val removedCatalogue = catalogues.removeChildCatalogue(catalogue.id, secondPresavedSubcatalogue)

        assertThat(removedCatalogue, Is(secondPresavedSubcatalogue.copy(parentId = null)))
        assertThat(presavedCatalogue.childrenIds, Is(mapOf(firstPresavedSubcatalogue.id to 0)))
    }

    @Test
    fun `Reorder subcatalogues after deletion`() {
        catalogues.removeChildCatalogue(catalogue.id, firstPresavedSubcatalogue)

        assertThat(presavedCatalogue.childrenIds, Is(mapOf(secondPresavedSubcatalogue.id to 0)))
    }

    @Test(expected = CatalogueNotAChildException::class)
    fun `Removing subcatalogue from non-parent throws exception`() {
        catalogues.removeChildCatalogue("::fake-parent-catalogue-id::", catalogue)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Removing subcatalogue from non-existent parent throws exception`() {
        db.getRepository(collectionName, Catalogue::class.java).remove(Catalogue::id eq catalogue.id)
        catalogues.removeChildCatalogue(catalogue.id, firstPresavedSubcatalogue)
    }

    @Test
    fun `Switch subcatalogues in catalogue`() {
        val updatedCatalogue = catalogues.switchChildCatalogues(catalogue.id,
                firstPresavedSubcatalogue, secondPresavedSubcatalogue)

        assertThat(updatedCatalogue, Is(presavedCatalogue.copy(childrenIds = mapOf(
                secondPresavedSubcatalogue.id to 0,
                firstPresavedSubcatalogue.id to 1
        ))))
    }

    @Test(expected = CatalogueNotAChildException::class)
    fun `Switching subcatalogues in non-containing catalogue throws exception`() {
        catalogues.switchChildCatalogues(catalogue.id, subCatalogue, secondPresavedSubcatalogue)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Switching subcatalogues in non-existing catalogue throws exception`() {
        catalogues.switchChildCatalogues("::fake-catalogue-id::",
                firstPresavedSubcatalogue, secondPresavedSubcatalogue)
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
    fun `Deleting catalogue with subcatalogues folds their hierarchy ot its parent`() {
        val deletedCatalogue = catalogues.delete(presavedCatalogue.id)

        val subCatalogue = db.getRepository(collectionName, Catalogue::class.java)
                .find(Catalogue::id eq deletedCatalogue.childrenIds.keys.first()).first()

        assertThat(subCatalogue.parentId, Is(deletedCatalogue.parentId))
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Deleting non-existent catalogue throws exception`() {
        catalogues.delete("::fake-catalogue-id::")
    }
}
