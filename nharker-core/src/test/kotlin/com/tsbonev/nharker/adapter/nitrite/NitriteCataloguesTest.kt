package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.core.CatalogueRequest
import com.tsbonev.nharker.core.exceptions.*
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.nitrite
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteCataloguesTest {

    private val db = nitrite { }

    private val instant = LocalDateTime.of(1, 1, 1, 1, 1, 1)
    private val collectionName = "TestCatalogues"

    private val firstPresavedArticle = Article(
            "::firstArticleId::",
            "article-title-1",
            "Article title 1",
            instant,
            "::catalogue-id::"
    )

    private val secondPresavedArticle = Article(
            "::secondArticleId::",
            "article-title-2",
            "Article title 2",
            instant,
            "::catalogue-id::"
    )

    private val article = Article(
            "::articleId::",
            "article-title",
            "Article title",
            instant,
            "::default-catalogue::"
    )

    private val firstPresavedSubcatalogue = Catalogue(
            "::catalogue-id-1::",
            "::catalogue-title-1::",
            instant,
            parentCatalogue = "::catalogue-id::"
    )

    private val secondPresavedSubcatalogue = Catalogue(
            "::catalogue-id-2::",
            "::catalogue-title-2::",
            instant,
            parentCatalogue = "::catalogue-id::"
    )

    private val subCatalogue = Catalogue(
            "::catalogue-id-3::",
            "::catalogue-title-3::",
            instant
    )

    private val catalogueRequest = CatalogueRequest(
            "::catalogue-title::"
    )

    private val catalogue = Catalogue(
            "::catalogue-id::",
            "::catalogue-title::",
            instant,
            mapOf(firstPresavedArticle.id to 0,
                    secondPresavedArticle.id to 1),
            mapOf(firstPresavedSubcatalogue.id to 0,
                    secondPresavedSubcatalogue.id to 1)
    )

    private val catalogues = NitriteCatalogues(
            db,
            collectionName
    ) {instant}

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

    @Test
    fun `Save and return catalogue`(){
        db.getRepository(collectionName, Catalogue::class.java).remove(Catalogue::id eq catalogue.id)

        assertThat(catalogues.save(catalogue), Is(catalogue))
    }

    @Test(expected = CatalogueTitleTakenException::class)
    fun `Creating a catalogue with a taken title throws exception`(){
        catalogues.create(catalogueRequest)
    }

    @Test
    fun `Change catalogue title`(){
        val updatedCatalogue = catalogues.changeTitle(catalogue.id, "::new-title::")

        assertThat(updatedCatalogue, Is(catalogue.copy(title = updatedCatalogue.title)))
        assertThat(presavedCatalogue(), Is(updatedCatalogue))
    }

    @Test(expected = CatalogueTitleTakenException::class)
    fun `Changing catalogue title to taken one throws exception`(){
        catalogues.changeTitle(catalogue.id, catalogue.title)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Changing title of non-existent catalogue throws exception`(){
        catalogues.changeTitle("::fake-id::", catalogue.title)
    }

    @Test
    fun `Change parent of catalogue`(){
        val parentChildPair = catalogues.changeParentCatalogue(catalogue.id, subCatalogue.id)

        assertThat(parentChildPair.first, Is(subCatalogue.copy(subCatalogues = mapOf(catalogue.id to 0))))
        assertThat(parentChildPair.second, Is(presavedCatalogue()))
        assertThat(presavedCatalogue().parentCatalogue, Is(parentChildPair.first.id))
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Changing parent of catalogue to non-existent throws exception`(){
        catalogues.changeParentCatalogue(catalogue.id, "::fake-parent-id::")
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Changing parent of non-existent catalogue throws exception`(){
        catalogues.changeParentCatalogue("::fake-parent-id::", subCatalogue.id)
    }

    @Test(expected = CatalogueAlreadyAChildException::class)
    fun `Changing parent of catalogue to the same value throws exception`(){
        catalogues.changeParentCatalogue(catalogue.id, catalogue.parentCatalogue)
    }

    @Test
    fun `Append catalogue to catalogue subcatalogues`(){
        val appendedChild = catalogues.appendSubCatalogue(catalogue.id, subCatalogue.id)

        assertThat(appendedChild, Is(subCatalogue.copy(parentCatalogue = catalogue.id)))
        assertThat(presavedCatalogue(), Is(catalogue.copy(subCatalogues = catalogue.subCatalogues.plus(
                subCatalogue.id to catalogue.subCatalogues.count()
        ))))
    }

    @Test(expected = CatalogueAlreadyAChildException::class)
    fun `Appending catalogue that is already a subcatalogue throws exception`(){
        catalogues.appendSubCatalogue(catalogue.id, firstPresavedSubcatalogue.id)
    }

    @Test(expected = SelfContainedCatalogueException::class)
    fun `Appending catalogue to itself throws exception`(){
        catalogues.appendSubCatalogue(catalogue.id, catalogue.id)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Appending a non-existent catalogue throws exception`(){
        catalogues.appendSubCatalogue(catalogue.id, "::fake-catalogue-id::")
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Appending subcatalogue to non-existent catalogue throws exception`(){
        catalogues.appendSubCatalogue("::fake-catalogue-id::", catalogue.id)
    }

    @Test
    fun `Remove subcatalogue from catalogue`(){
        val removedCatalogue = catalogues.removeSubCatalogue(catalogue.id, secondPresavedSubcatalogue.id)

        assertThat(removedCatalogue, Is(secondPresavedSubcatalogue.copy(parentCatalogue = "None")))
        assertThat(presavedCatalogue().subCatalogues, Is(mapOf(firstPresavedSubcatalogue.id to 0)))
    }

    @Test
    fun `Reorder subcatalogues after deletion`(){
        catalogues.removeSubCatalogue(catalogue.id, firstPresavedSubcatalogue.id)

        assertThat(presavedCatalogue().subCatalogues, Is(mapOf(secondPresavedSubcatalogue.id to 0)))
    }

    @Test(expected = CatalogueNotAChildException::class)
    fun `Removing subcatalogue from non-parent throws exception`(){
        catalogues.removeSubCatalogue("::fake-parent-catalogue-id::", catalogue.id)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Removing non-existent catalogue throws exception`(){
        catalogues.removeSubCatalogue(catalogue.id, "::fake-catalogue-id::")
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Removing subcatalogue from non-existent parent throws exception`(){
        db.getRepository(collectionName, Catalogue::class.java).remove(Catalogue::id eq catalogue.id)
        catalogues.removeSubCatalogue(catalogue.id, firstPresavedSubcatalogue.id)
    }

    @Test
    fun `Append article to catalogue`(){
        val appendedChild = catalogues.appendArticle(catalogue.id, article)

        assertThat(appendedChild, Is(article.copy(catalogueId = catalogue.id)))
        assertThat(presavedCatalogue(), Is(catalogue.copy(articles = catalogue.articles.plus(
                article.id to catalogue.articles.count()
        ))))
    }

    @Test(expected = ArticleAlreadyInCatalogueException::class)
    fun `Appending article that is already a in a catalogue throws exception`(){
        catalogues.appendArticle(catalogue.id, firstPresavedArticle)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Appending article to non-existent catalogue throws exception`(){
        catalogues.appendArticle("::fake-catalogue-id::", article)
    }

    @Test
    fun `Remove article from catalogue`(){
        val removedArticle = catalogues.removeArticle(catalogue.id, secondPresavedArticle)

        assertThat(removedArticle, Is(secondPresavedArticle.copy(catalogueId = "none")))
        assertThat(presavedCatalogue(), Is(catalogue.copy(articles = mapOf(firstPresavedArticle.id to 0))))
    }

    @Test
    fun `Reorder articles after deletion`(){
        catalogues.removeArticle(catalogue.id, firstPresavedArticle)

        assertThat(presavedCatalogue(), Is(catalogue.copy(articles = mapOf(secondPresavedArticle.id to 0))))
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
        val updatedCatalogue = catalogues.switchArticles(catalogue.id, firstPresavedArticle, secondPresavedArticle)

        assertThat(updatedCatalogue, Is(presavedCatalogue().copy(articles = mapOf(
                secondPresavedArticle.id to 0,
                firstPresavedArticle.id to 1
        ))))
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Switching articles in non-existent catalogue throws exception`(){
        catalogues.switchArticles("::fake-catalogue-id::", firstPresavedArticle, secondPresavedArticle)
    }

    @Test(expected = ArticleNotInCatalogueException::class)
    fun `Switching articles in non-containing catalogue throws exception`(){
        catalogues.switchArticles(catalogue.id, article, secondPresavedArticle)
    }

    @Test
    fun `Switch subcatalogues in catalogue`(){
        val updatedCatalogue = catalogues.switchSubCatalogues(catalogue.id, firstPresavedSubcatalogue, secondPresavedSubcatalogue)

        assertThat(updatedCatalogue, Is(presavedCatalogue().copy(subCatalogues = mapOf(
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
        catalogues.switchSubCatalogues("::fake-catalogue-id::", firstPresavedSubcatalogue, secondPresavedSubcatalogue)
    }

    private fun presavedCatalogue(): Catalogue{
        return db.getRepository(collectionName, Catalogue::class.java).find(Catalogue::id eq catalogue.id).first()
    }
}
