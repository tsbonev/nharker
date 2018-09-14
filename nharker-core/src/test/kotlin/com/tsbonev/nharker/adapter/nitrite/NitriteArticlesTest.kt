package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.*
import com.tsbonev.nharker.core.exceptions.*
import com.tsbonev.nharker.core.helpers.append
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
class NitriteArticlesTest {

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

    val defaultCatalogue = Catalogue(
            "::default-catalogue::",
            "Default catalogue",
            instant
    )

    private val articles = NitriteArticles(db, collectionName) {instant}

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
        val appendedEntry = articles.appendEntry(article.id, entry)

        assertThat(appendedEntry, Is(entry))
        assertThat(presavedArticle(), Is(article.copy(entries = article.entries.plus(entry.id to 2))))
    }

    @Test(expected = EntryAlreadyInArticleException::class)
    fun `Appending entry that is already in article throws exception`(){
        articles.appendEntry(article.id, firstPresavedEntry)
    }

    @Test
    fun `Remove and return entry from article`(){
        val removedEntry = articles.removeEntry(article.id, secondPresavedEntry)

        assertThat(removedEntry, Is(secondPresavedEntry.copy(articleId = "deleted")))
        assertThat(presavedArticle().entries.count(), Is(1))
    }

    @Test
    fun `Reorder entries after deletion`(){
        articles.removeEntry(article.id, firstPresavedEntry)

        assertThat(presavedArticle().entries.count(), Is(1))
        assertThat(presavedArticle().entries[secondPresavedEntry.id], Is(0))
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Deleting from a non-existent article throws exception`(){
        articles.removeEntry("::fake-article-id::", firstPresavedEntry)
    }

    @Test(expected = EntryNotInArticleException::class)
    fun `Deleting an entry that isn't in an article throws exception`(){
        articles.removeEntry(article.id, entry)
    }

    @Test
    fun `Change article catalogue`(){
        val changedCatalogue = articles.setCatalogue(article.id, catalogue)

        assertThat(presavedArticle(), Is(article.copy(catalogueId = catalogue.id)))
        assertThat(changedCatalogue, Is(catalogue.copy(articles= catalogue.articles.append(article.id))))
    }

    @Test(expected = ArticleAlreadyInCatalogueException::class)
    fun `Changing catalogue of article to the same value throws exception`(){
        articles.setCatalogue(article.id, defaultCatalogue)
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Changing catalogue of non-existent article throws exception`(){
        articles.setCatalogue("::fake-article-id::", catalogue)
    }

    private fun presavedArticle(): Article{
        return db.getRepository(collectionName, Article::class.java).find(Article::id eq article.id).first()
    }
}