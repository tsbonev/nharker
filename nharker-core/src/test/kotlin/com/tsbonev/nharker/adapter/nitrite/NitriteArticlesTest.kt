package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.*
import com.tsbonev.nharker.core.exceptions.*
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.nitrite
import org.jmock.AbstractExpectations.returnValue
import org.jmock.Expectations
import org.jmock.Mockery
import org.jmock.integration.junit4.JUnitRuleMockery
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat
import org.junit.Rule
import java.util.Optional

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteArticlesTest {

    @Rule
    @JvmField
    val context: JUnitRuleMockery = JUnitRuleMockery()

    private fun Mockery.expecting(block: Expectations.() -> Unit){
            checking(Expectations().apply(block))
    }

    private val db = nitrite { }

    private val instant = LocalDateTime.of(1, 1, 1, 1, 1, 1)
    private val collectionName = "TestArticles"

    private val catalogue = Catalogue(
            "::catalogueId::",
            "::title::",
            instant
    )

    private val entry = Entry(
            "::entryId::",
            instant,
            "::articleId::",
            "::content::",
            mapOf("::content::" to "::article::")
    )

    private val firstPresavedEntry = Entry(
            "::firstPresavedEntryId::",
            instant,
            "::articleId::",
            "::content::",
            emptyMap()
    )

    private val secondPresavedEntry = Entry(
            "::secondPresavedEntryId::",
            instant,
            "::articleId::",
            "::content::",
            emptyMap()
    )


    private val articleRequest = ArticleRequest(
            "Article title",
            "::default-catalogue::"
    )

    private val article = Article(
            "::articleId::",
            "article-title",
            "Article title",
            instant,
            "::default-catalogue::",
            emptyList(),
            mapOf(firstPresavedEntry.id to 0,
                    secondPresavedEntry.id to 1),
            ArticleLinks(mutableMapOf())
    )

    private val entryService = context.mock(EntryService::class.java)

    private val catalogueService = context.mock(CatalogueService::class.java)

    private val articles = NitriteArticles(db, entryService, catalogueService, collectionName) {instant}

    @Before
    fun setUp(){
        db.getRepository(collectionName, Article::class.java).insert(article)
    }

    @Test
    fun `Create and return article`(){
        db.getRepository(collectionName, Article::class.java).remove(Article::linkTitle eq article.linkTitle)
        val createdArticle = articles.create(articleRequest)

        assertThat(createdArticle.copy(id = "::articleId::", entries = article.entries), Is(article))
    }

    @Test(expected = ArticleTitleTakenException::class)
    fun `Creating an article with a taken title throws exception`(){
        articles.create(articleRequest)
    }

    @Test
    fun `Retrieve article by id`(){
        assertThat(articles.getById(article.id).isPresent, Is(true))
        assertThat(articles.getById(article.id).get(), Is(article))
    }

    @Test
    fun `Return empty when article isn't found`(){
        assertThat(articles.getById("::fake-article-id::").isPresent, Is(false))
    }

    @Test
    fun `Append entry to article`(){
        context.expecting {
            oneOf(entryService).getById(entry.id)
            will(returnValue(Optional.of(entry)))

            oneOf(entryService).changeArticle(entry.id, article.id)
            will(returnValue(entry))
        }

        val appendedEntry = articles.appendEntry(article.id, entry.id)

        assertThat(appendedEntry, Is(entry))
        assertThat(presavedArticle(), Is(article.copy(entries = article.entries.plus(entry.id to 2))))
    }

    @Test(expected = EntryAlreadyInArticleException::class)
    fun `Appending entry that is already in article throws exception`(){
        articles.appendEntry(article.id, firstPresavedEntry.id)
    }

    @Test
    fun `Remove and return entry from article`(){
        context.expecting {
            oneOf(entryService).getById(secondPresavedEntry.id)
            will(returnValue(Optional.of(secondPresavedEntry)))

            oneOf(entryService).changeArticle(secondPresavedEntry.id, "deleted")
            will(returnValue(secondPresavedEntry.copy(articleId = "deleted")))
        }

        val removedEntry = articles.removeEntry(article.id, secondPresavedEntry.id)

        assertThat(removedEntry, Is(secondPresavedEntry.copy(articleId = "deleted")))
        assertThat(presavedArticle().entries.count(), Is(1))
    }

    @Test
    fun `Reorder entries after deletion`(){
        context.expecting {
            oneOf(entryService).getById(firstPresavedEntry.id)
            will(returnValue(Optional.of(firstPresavedEntry)))

            oneOf(entryService).changeArticle(firstPresavedEntry.id, "deleted")
            will(returnValue(firstPresavedEntry.copy(articleId = "deleted")))
        }

        articles.removeEntry(article.id, firstPresavedEntry.id)

        assertThat(presavedArticle().entries.count(), Is(1))
        assertThat(presavedArticle().entries[secondPresavedEntry.id], Is(0))
    }

    @Test(expected = EntryNotFoundException::class)
    fun `Deleting a non-existent entry throws exception`(){
        context.expecting {
            oneOf(entryService).getById("::fake-entry-id::")
            will(returnValue(Optional.empty<Entry>()))
        }

        articles.removeEntry(article.id, "::fake-entry-id::")
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Deleting from a non-existent article throws exception`(){
        articles.removeEntry("::fake-article-id::", "::fake-entry-id::")
    }

    @Test(expected = EntryNotInArticleException::class)
    fun `Deleting an entry that isn't in an article throws exception`(){
        context.expecting {
            oneOf(entryService).getById(entry.id)
            will(returnValue(Optional.of(entry)))
        }

        articles.removeEntry(article.id, entry.id)
    }

    @Test
    fun `Change article catalogue`(){
        context.expecting {
            oneOf(catalogueService).getById(catalogue.id)
            will(returnValue(Optional.of(catalogue)))
        }

        val updatedArticle = articles.setCatalogue(article.id, catalogue.id)

        assertThat(presavedArticle(), Is(updatedArticle))
    }

    @Test(expected = ArticleAlreadyInCatalogueException::class)
    fun `Changing catalogue of article to the same value throws exception`(){
        articles.setCatalogue(article.id, "::default-catalogue::")
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Changing catalogue of non-existent article throws exception`(){
        articles.setCatalogue("::fake-article-id::", catalogue.id)
    }

    @Test(expected = CatalogueNotFoundException::class)
    fun `Changing catalogue to non-existent one throws exception`(){
        context.expecting {
            oneOf(catalogueService).getById("::fake-catalogue::")
            will(returnValue(Optional.empty<Catalogue>()))
        }

        articles.setCatalogue(article.id, "::fake-catalogue::")
    }

    private fun presavedArticle(): Article{
        return db.getRepository(collectionName, Article::class.java).find(Article::id eq article.id).first()
    }
}