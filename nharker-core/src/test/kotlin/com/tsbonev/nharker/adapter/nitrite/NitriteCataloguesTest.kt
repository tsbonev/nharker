package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.*
import com.tsbonev.nharker.core.helpers.StubClock
import com.tsbonev.nharker.core.helpers.append
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.nitrite
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat
import java.time.Instant
import java.time.ZoneOffset

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteCataloguesTest {

    private val db = nitrite { }

    private val date = LocalDateTime.ofInstant(Instant.ofEpochSecond(1), ZoneOffset.UTC)
    private val stubClock = StubClock()
    private val collectionName = "TestCatalogues"

    private val firstPresavedArticle = Article(
            "::firstArticleId::",
            "article-title-1",
            "Article title 1",
            date
    )

    private val secondPresavedArticle = Article(
            "::secondArticleId::",
            "article-title-2",
            "Article title 2",
            date
    )

    private val article = Article(
            "::articleId::",
            "article-title",
            "Article title",
            date
    )

    private val firstPresavedSubcatalogue = Catalogue(
            "::catalogue-value-1::",
            "::catalogue-title-1::",
            date,
            parentCatalogue = "::catalogue-value::"
    )

    private val secondPresavedSubcatalogue = Catalogue(
            "::catalogue-value-2::",
            "::catalogue-title-2::",
            date,
            parentCatalogue = "::catalogue-value::"
    )

    private val subCatalogue = Catalogue(
            "::catalogue-value-3::",
            "::catalogue-title-3::",
            date
    )

    private val catalogueRequest = CatalogueRequest(
            "::catalogue-title::"
    )

    private val catalogue = Catalogue(
            "::catalogue-value::",
            "::catalogue-title::",
            date,
            mapOf(firstPresavedArticle.id to 0,
                    secondPresavedArticle.id to 1),
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
    fun setUp(){
        db.getRepository(collectionName, Catalogue::class.java).insert(catalogue)
        db.getRepository(collectionName, Catalogue::class.java).insert(subCatalogue)
        db.getRepository(collectionName, Catalogue::class.java).insert(firstPresavedSubcatalogue)
        db.getRepository(collectionName, Catalogue::class.java).insert(secondPresavedSubcatalogue)
    }

    @Test
    fun `Create and return catalogue`(){
        db.getRepository(collectionName, Catalogue::class.java).remove(Catalogue::id eq catalogue.id)

        assertThat(catalogues.create(catalogueRequest).copy(id = catalogue.id),
                Is(catalogue.copy(subCatalogues = emptyMap(),
                        articles = emptyMap())))
    }

    @Test(expected = CatalogueTitleTakenException::class)
    fun `Creating a catalogue with a taken title throws exception`(){
        catalogues.create(catalogueRequest)
    }

    @Test
    fun `Save and return catalogue`(){
        db.getRepository(collectionName, Catalogue::class.java).remove(Catalogue::id eq catalogue.id)
        
        assertThat(catalogues.save(catalogue), Is(catalogue))
    }

    @Test
    fun `Retrieve catalogue by id`(){
        assertThat(catalogues.getById(catalogue.id).isPresent, Is(true))
        assertThat(catalogues.getById(catalogue.id).get(), Is(catalogue))
    }

    @Test
    fun `Retrieving non-existent catalogue returns empty`(){
        assertThat(catalogues.getById("fake-catalogue-value::").isPresent, Is(false))
    }

    @Test
    fun `Change catalogue title`(){
        val updatedCatalogue = catalogues.changeTitle(catalogue.id, "::new-title::")

        assertThat(updatedCatalogue, Is(catalogue.copy(title = updatedCatalogue.title)))
        assertThat(presavedCatalogue, Is(updatedCatalogue))
    }

    @Test(expected = CatalogueTitleTakenException::class)
    fun `Changing catalogue title to taken one throws exception`(){
        catalogues.changeTitle(catalogue.id, catalogue.title)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Changing title of non-existent catalogue throws exception`(){
        catalogues.changeTitle("::fake-value::", catalogue.title)
    }

    @Test
    fun `Change parent of catalogue`(){
        val updatedChild = catalogues.changeParentCatalogue(subCatalogue.id, catalogue)

        assertThat(updatedChild, Is(subCatalogue.copy(
                parentCatalogue = catalogue.id)))
        assertThat(presavedCatalogue, Is(catalogue.copy(
                subCatalogues= catalogue.subCatalogues.append(subCatalogue.id))))
    }

    @Test(expected = CatalogueAlreadyAChildException::class)
    fun `Changing parent of a child to the same parent throws exception`() {
        catalogues.changeParentCatalogue(firstPresavedSubcatalogue.id, catalogue)
    }

    @Test(expected = CatalogueCircularInheritanceException::class)
    fun `Changing parent of a parent to its child throws exception`(){
        catalogues.changeParentCatalogue(catalogue.id, firstPresavedSubcatalogue)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Changing parent of non-existent catalogue throws exception`(){
        catalogues.changeParentCatalogue("::fake-parent-id::", subCatalogue)
    }

    @Test(expected = SelfContainedCatalogueException::class)
    fun `Changing parent of catalogue to the same value throws exception`(){
        catalogues.changeParentCatalogue(catalogue.id, catalogue)
    }

    @Test
    fun `Append catalogue to catalogue subcatalogues`(){
        val appendedChild = catalogues.appendSubCatalogue(catalogue.id, subCatalogue)

        assertThat(appendedChild, Is(subCatalogue.copy(parentCatalogue = catalogue.id)))
        assertThat(presavedCatalogue, Is(catalogue.copy(subCatalogues = catalogue.subCatalogues.plus(
                subCatalogue.id to catalogue.subCatalogues.count()
        ))))
    }

    @Test(expected = CatalogueCircularInheritanceException::class)
    fun `Appending parent to its own child throws exception`(){
        catalogues.appendSubCatalogue(firstPresavedSubcatalogue.id, catalogue)
    }

    @Test(expected = CatalogueAlreadyAChildException::class)
    fun `Appending catalogue that is already a subcatalogue throws exception`(){
        catalogues.appendSubCatalogue(catalogue.id, firstPresavedSubcatalogue)
    }

    @Test(expected = SelfContainedCatalogueException::class)
    fun `Appending catalogue to itself throws exception`(){
        catalogues.appendSubCatalogue(catalogue.id, catalogue)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Appending subcatalogue to non-existent catalogue throws exception`(){
        catalogues.appendSubCatalogue("::fake-catalogue-id::", catalogue)
    }

    @Test
    fun `Remove subcatalogue from catalogue`(){
        val removedCatalogue = catalogues.removeSubCatalogue(catalogue.id, secondPresavedSubcatalogue)

        assertThat(removedCatalogue, Is(secondPresavedSubcatalogue.copy(parentCatalogue = null)))
        assertThat(presavedCatalogue.subCatalogues, Is(mapOf(firstPresavedSubcatalogue.id to 0)))
    }

    @Test
    fun `Reorder subcatalogues after deletion`(){
        catalogues.removeSubCatalogue(catalogue.id, firstPresavedSubcatalogue)

        assertThat(presavedCatalogue.subCatalogues, Is(mapOf(secondPresavedSubcatalogue.id to 0)))
    }

    @Test(expected = CatalogueNotAChildException::class)
    fun `Removing subcatalogue from non-parent throws exception`(){
        catalogues.removeSubCatalogue("::fake-parent-catalogue-id::", catalogue)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Removing subcatalogue from non-existent parent throws exception`(){
        db.getRepository(collectionName, Catalogue::class.java).remove(Catalogue::id eq catalogue.id)
        catalogues.removeSubCatalogue(catalogue.id, firstPresavedSubcatalogue)
    }

    @Test
    fun `Append article to catalogue`(){
        val appendedChild = catalogues.appendArticle(catalogue.id, article)

        assertThat(appendedChild, Is(article))
        assertThat(presavedCatalogue, Is(catalogue.copy(articles = catalogue.articles.plus(
                article.id to catalogue.articles.count()
        ))))
    }

    @Test(expected = ArticleAlreadyInCatalogueException::class)
    fun `Appending article that is already a in a catalogue throws exception`(){
        catalogues.appendArticle(catalogue.id, firstPresavedArticle)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Appending article to non-existent catalogue throws exception`(){
        catalogues.appendArticle("::fake-catalogue-value::", article)
    }

    @Test
    fun `Remove article from catalogue`(){
        val removedArticle = catalogues.removeArticle(catalogue.id, secondPresavedArticle)

        assertThat(removedArticle, Is(secondPresavedArticle))
        assertThat(presavedCatalogue, Is(catalogue.copy(articles = mapOf(firstPresavedArticle.id to 0))))
    }

    @Test
    fun `Reorder articles after deletion`(){
        catalogues.removeArticle(catalogue.id, firstPresavedArticle)

        assertThat(presavedCatalogue, Is(catalogue.copy(articles = mapOf(secondPresavedArticle.id to 0))))
    }

    @Test(expected = ArticleNotInCatalogueException::class)
    fun `Removing article from non-parent throws exception`(){
        catalogues.removeArticle(catalogue.id, article)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Removing article from non-existent catalogue throws exception`(){
        db.getRepository(collectionName, Catalogue::class.java).remove(Catalogue::id eq catalogue.id)
        catalogues.removeArticle(catalogue.id, firstPresavedArticle)
    }

    @Test
    fun `Switch articles in catalogue`(){
        val updatedCatalogue = catalogues.switchArticles(catalogue.id,
                firstPresavedArticle, secondPresavedArticle)

        assertThat(updatedCatalogue, Is(presavedCatalogue.copy(articles = mapOf(
                secondPresavedArticle.id to 0,
                firstPresavedArticle.id to 1
        ))))
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Switching articles in non-existent catalogue throws exception`(){
        catalogues.switchArticles("::fake-catalogue-value::",
                firstPresavedArticle, secondPresavedArticle)
    }

    @Test(expected = ArticleNotInCatalogueException::class)
    fun `Switching articles in non-containing catalogue throws exception`(){
        catalogues.switchArticles(catalogue.id, article, secondPresavedArticle)
    }

    @Test
    fun `Switch subcatalogues in catalogue`(){
        val updatedCatalogue = catalogues.switchSubCatalogues(catalogue.id,
                firstPresavedSubcatalogue, secondPresavedSubcatalogue)

        assertThat(updatedCatalogue, Is(presavedCatalogue.copy(subCatalogues = mapOf(
                secondPresavedSubcatalogue.id to 0,
                firstPresavedSubcatalogue.id to 1
        ))))
    }

    @Test(expected = CatalogueNotAChildException::class)
    fun `Switching subcatalogues in non-containing catalogue throws exception`(){
        catalogues.switchSubCatalogues(catalogue.id, subCatalogue, secondPresavedSubcatalogue)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Switching subcatalogues in non-existing catalogue throws exception`(){
        catalogues.switchSubCatalogues("::fake-catalogue-value::",
                firstPresavedSubcatalogue, secondPresavedSubcatalogue)
    }

    @Test
    fun `Delete and return catalogue`(){
        val savedCatalogue = presavedCatalogue

        val deletedCatalogue = catalogues.delete(savedCatalogue.id)

        assertThat(savedCatalogue, Is(deletedCatalogue))
        assertThat(
                db.getRepository(collectionName, Catalogue::class.java)
                        .find(Catalogue::id eq catalogue.id).firstOrNull(),
                Is(nullValue()))
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Deleting non-existent catalogue throws exception`(){
        catalogues.delete("::fake-catalogue-value::")
    }
}
