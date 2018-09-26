package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.*
import com.tsbonev.nharker.core.helpers.StubClock
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.nitrite
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
class NitriteArticlesTest {

    private val db = nitrite { }

    private val date = LocalDateTime.ofInstant(Instant.ofEpochSecond(1), ZoneOffset.UTC)
    private val stubClock = StubClock()
    private val collectionName = "TestArticles"

    private val entry = Entry(
            "::entryId::",
            date,
            "::content::",
            mapOf("::content::" to "::article::")
    )

    private val firstPresavedEntry = Entry(
            "::firstPresavedEntryId::",
            date,
            "::content::",
            emptyMap()
    )

    private val secondPresavedEntry = Entry(
            "::secondPresavedEntryId::",
            date,
            "::content::",
            emptyMap()
    )

    private val articleProperty = Entry(
            "::articleProperty::",
            date,
            "::Content::",
            emptyMap()
    )

    private val articleRequest = ArticleRequest(
            "Article title"
    )

    private val article = Article(
            "::articleId::",
            "article-title",
            "Article title",
            date,
            properties = ArticleProperties(mutableMapOf("::property::" to articleProperty)),
            entries = mapOf(firstPresavedEntry.id to 0,
                    secondPresavedEntry.id to 1),
            links = ArticleLinks(mutableMapOf())
    )

    private val articles = NitriteArticles(db, collectionName, clock = stubClock)

    @Before
    fun setUp(){
        db.getRepository(collectionName, Article::class.java).insert(article)
    }

    @Test
    fun `Create and return article`(){
        db.getRepository(collectionName, Article::class.java)
                .remove(Article::linkTitle eq article.linkTitle)

        val createdArticle = articles.create(articleRequest)

        assertThat(createdArticle.copy(id = "::articleId::",
                entries = article.entries),
                Is(article.copy(properties = ArticleProperties(mutableMapOf()))))
    }

    @Test
    fun `Save and return article`(){
        db.getRepository(collectionName, Article::class.java).remove(Article::linkTitle eq article.linkTitle)
        assertThat(articles.save(article), Is(article))
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
        assertThat(articles.getById("::fake-article-value::").isPresent, Is(false))
    }

    @Test
    fun `Append entry to article`(){
        val appendedEntry = articles.appendEntry(article.id, entry)

        assertThat(appendedEntry, Is(entry))
        assertThat(presavedArticle, Is(article.copy(entries = article.entries.plus(entry.id to 2))))
    }

    @Test(expected = EntryAlreadyInArticleException::class)
    fun `Appending entry that is already in article throws exception`(){
        articles.appendEntry(article.id, firstPresavedEntry)
    }

    @Test
    fun `Remove and return entry from article`(){
        val removedEntry = articles.removeEntry(article.id, secondPresavedEntry)

        assertThat(removedEntry, Is(secondPresavedEntry))
        assertThat(presavedArticle.entries.count(), Is(1))
    }

    @Test
    fun `Reorder entries after deletion`(){
        articles.removeEntry(article.id, firstPresavedEntry)

        assertThat(presavedArticle.entries.count(), Is(1))
        assertThat(presavedArticle.entries[secondPresavedEntry.id], Is(0))
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Deleting from a non-existent article throws exception`(){
        articles.removeEntry("::fake-article-value::", firstPresavedEntry)
    }

    @Test(expected = EntryNotInArticleException::class)
    fun `Deleting an entry that isn't in an article throws exception`(){
        articles.removeEntry(article.id, entry)
    }

    @Test
    fun `Switch entries in an article`(){
        val updatedArticle = articles.switchEntries(article.id, firstPresavedEntry, secondPresavedEntry)

        assertThat(updatedArticle, Is(presavedArticle.copy(
                entries = mapOf(secondPresavedEntry.id to 0,
                        firstPresavedEntry.id to 1)
        )))
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Switching entries of non-existing article throws exception`(){
        articles.switchEntries("::fake-article-value::", firstPresavedEntry, secondPresavedEntry)
    }

    @Test(expected = EntryNotInArticleException::class)
    fun `Switching entries of non-containing article throws exception`(){
        articles.switchEntries(article.id, entry, secondPresavedEntry)
    }

    @Test
    fun `Attach property to article`(){
        val attachedProperty = articles.attachProperty(article.id, "::propertyName::", entry)

        assertThat(attachedProperty, Is(entry))
        assertThat(presavedArticle.properties.getAll(),
                Is(mapOf("::propertyName::" to entry,
                        "::property::" to articleProperty)))
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Attaching property to non-existent article throws exception`(){
        articles.attachProperty("::non-existing-article::", "::propertyName::", entry)
    }

    @Test
    fun `Detach property from article`(){
        val detachedProperty = articles.detachProperty(article.id, "::property::")

        assertThat(detachedProperty, Is(articleProperty))
        assertThat(presavedArticle.properties.getAll(), Is(emptyMap()))
    }
    
    @Test(expected = PropertyNotFoundException::class)
    fun `Detaching non-existent property throws exception`(){
        articles.detachProperty(article.id, "::non-existent-property::")
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Detaching property from non-existent article throws exception`(){
        articles.detachProperty("::non-existing-article::", "::property::")
    }

    private val presavedArticle: Article
            get() = db.getRepository(collectionName, Article::class.java).find(Article::id eq article.id).first()
}