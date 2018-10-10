package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.*
import com.tsbonev.nharker.core.helpers.StubClock
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.nitrite
import org.hamcrest.CoreMatchers.nullValue
import org.jmock.AbstractExpectations.returnValue
import org.jmock.Expectations
import org.jmock.Mockery
import org.jmock.integration.junit4.JUnitRuleMockery
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteArticlesTest {

    @Rule
    @JvmField
    val context: JUnitRuleMockery = JUnitRuleMockery()

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
            links = ArticleLinks(mutableMapOf("article-title-1" to 2,
                    "article-title-2" to 1))
    )

    private val presavedArticle: Article
        get() = db.getRepository(collectionName, Article::class.java)
                .find(Article::id eq article.id).first()

    private val entryLinker = context.mock(EntryLinker::class.java)

    private val articles = NitriteArticles(db, collectionName, entryLinker, clock = stubClock)

    @Before
    fun setUp() {
        db.getRepository(collectionName, Article::class.java).insert(article)
    }

    @Test
    fun `Create and return article`() {
        db.getRepository(collectionName, Article::class.java)
                .remove(Article::linkTitle eq article.linkTitle)

        val createdArticle = articles.create(articleRequest)

        assertThat(createdArticle.copy(id = "::articleId::",
                entries = article.entries,
                links = article.links),
                Is(article.copy(properties = ArticleProperties(mutableMapOf()))))
    }

    @Test
    fun `Save and return article`() {
        db.getRepository(collectionName, Article::class.java)
                .remove(Article::linkTitle eq article.linkTitle)
        assertThat(articles.save(article), Is(article))
    }

    @Test(expected = ArticleTitleTakenException::class)
    fun `Creating an article with a taken title throws exception`() {
        articles.create(articleRequest)
    }

    @Test
    fun `Retrieve article by id`() {
        assertThat(articles.getById(article.id).isPresent, Is(true))
        assertThat(articles.getById(article.id).get(), Is(article))
    }

    @Test
    fun `Return empty when article isn't found by id`() {
        assertThat(articles.getById("::fake-article-value::").isPresent, Is(false))
    }

    @Test
    fun `Retrieve article by link title`(){
        assertThat(articles.getByLinkTitle(article.linkTitle).isPresent, Is(true))
        assertThat(articles.getByLinkTitle(article.linkTitle).get(), Is(article))
    }

    @Test
    fun `Return empty when article isn't found by link title`(){
        assertThat(articles.getByLinkTitle("::non-existing-link-title::").isPresent, Is(false))
    }

    @Test
    fun `Delete and return article`() {
        val deletedArticle = articles.delete(article.id)

        assertThat(deletedArticle, Is(article))
        assertThat(db.getRepository(collectionName, Article::class.java)
                .find(Article::id eq article.id).firstOrNull(), Is(nullValue()))
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Deleting non-existing article throws exception`() {
        articles.delete("::fake-article-id::")
    }

    @Test
    fun `Append entry to article`() {
        context.expecting {
            oneOf(entryLinker).findArticleLinks(entry, listOf("article-title"))
            will(returnValue(emptySet<String>()))
        }

        val appendedEntry = articles.appendEntry(article.id, entry)

        assertThat(appendedEntry, Is(entry))
        assertThat(presavedArticle, Is(article.copy(entries = article.entries.plus(entry.id to 2))))
    }

    @Test
    fun `Automatically link to articles when appending`() {
        context.expecting {
            oneOf(entryLinker).findArticleLinks(entry, listOf("article-title"))
            will(returnValue(setOf("new-article-title")))
        }

        articles.appendEntry(article.id, entry)
        article.links.addLink("new-article-title")

        assertThat(presavedArticle.links.contains("new-article-title"), Is(true))
        assertThat(presavedArticle.links.get("new-article-title"), Is(1))
    }

    @Test
    fun `Linking increases count when already linked`() {
        context.expecting {
            oneOf(entryLinker).findArticleLinks(entry, listOf("article-title"))
            will(returnValue(setOf("article-title-2")))
        }

        articles.appendEntry(article.id, entry)
        article.links.addLink("article-title-2")

        assertThat(presavedArticle.links.contains("article-title-2"), Is(true))
        assertThat(presavedArticle.links.get("article-title-2"), Is(2))
    }

    @Test(expected = EntryAlreadyInArticleException::class)
    fun `Appending entry that is already in article throws exception`() {
        articles.appendEntry(article.id, firstPresavedEntry)
    }

    @Test
    fun `Remove and return entry from article`() {
        context.expecting {
            oneOf(entryLinker).findArticleLinks(secondPresavedEntry, listOf("article-title"))
            will(returnValue(emptySet<String>()))
        }

        val removedEntry = articles.removeEntry(article.id, secondPresavedEntry)

        assertThat(removedEntry, Is(secondPresavedEntry))
        assertThat(presavedArticle.entries.count(), Is(1))
    }

    @Test
    fun `De-link when deleting entry`() {
        context.expecting {
            oneOf(entryLinker).findArticleLinks(firstPresavedEntry, listOf("article-title"))
            will(returnValue(setOf("article-title-2")))
        }

        articles.removeEntry(article.id, firstPresavedEntry)

        assertThat(presavedArticle.links.contains("article-title-2"), Is(false))
    }

    @Test
    fun `Decrease number of links when more than one is present`() {
        context.expecting {
            oneOf(entryLinker).findArticleLinks(firstPresavedEntry, listOf("article-title"))
            will(returnValue(setOf("article-title-1")))
        }

        articles.removeEntry(article.id, firstPresavedEntry)

        assertThat(presavedArticle.links.contains("article-title-1"), Is(true))
        assertThat(presavedArticle.links.get("article-title-1"), Is(1))
    }

    @Test
    fun `Reorder entries after deletion`() {
        context.expecting {
            oneOf(entryLinker).findArticleLinks(firstPresavedEntry, listOf("article-title"))
            will(returnValue(emptySet<String>()))
        }

        articles.removeEntry(article.id, firstPresavedEntry)

        assertThat(presavedArticle.entries.count(), Is(1))
        assertThat(presavedArticle.entries[secondPresavedEntry.id], Is(0))
    }

    @Test
    fun `Retrieve list of articles by querying full titles`(){
        val articleList = articles.searchByFullTitle(article.fullTitle)

        assertThat(articleList, Is(listOf(article)))
    }

    @Test
    fun `Return empty list when no articles match full title search`(){
        val articleList = articles.searchByFullTitle("Non existent")

        assertThat(articleList, Is(emptyList()))
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Deleting from a non-existent article throws exception`() {
        articles.removeEntry("::fake-article-value::", firstPresavedEntry)
    }

    @Test(expected = EntryNotInArticleException::class)
    fun `Deleting an entry that isn't in an article throws exception`() {
        articles.removeEntry(article.id, entry)
    }

    @Test
    fun `Switch entries in an article`() {
        val updatedArticle = articles.switchEntries(article.id, firstPresavedEntry, secondPresavedEntry)

        assertThat(updatedArticle, Is(presavedArticle.copy(
                entries = mapOf(secondPresavedEntry.id to 0,
                        firstPresavedEntry.id to 1)
        )))
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Switching entries of non-existing article throws exception`() {
        articles.switchEntries("::fake-article-value::", firstPresavedEntry, secondPresavedEntry)
    }

    @Test(expected = EntryNotInArticleException::class)
    fun `Switching entries of non-containing article throws exception`() {
        articles.switchEntries(article.id, entry, secondPresavedEntry)
    }

    @Test
    fun `Attach property to article`() {
        val attachedProperty = articles.attachProperty(article.id, "::propertyName::", entry)

        assertThat(attachedProperty, Is(entry))
        assertThat(presavedArticle.properties.getAll(),
                Is(mapOf("::propertyName::" to entry,
                        "::property::" to articleProperty)))
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Attaching property to non-existent article throws exception`() {
        articles.attachProperty("::non-existing-article::", "::propertyName::", entry)
    }

    @Test
    fun `Detach property from article`() {
        val detachedProperty = articles.detachProperty(article.id, "::property::")

        assertThat(detachedProperty, Is(articleProperty))
        assertThat(presavedArticle.properties.getAll(), Is(emptyMap()))
    }

    @Test(expected = PropertyNotFoundException::class)
    fun `Detaching non-existent property throws exception`() {
        articles.detachProperty(article.id, "::non-existent-property::")
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Detaching property from non-existent article throws exception`() {
        articles.detachProperty("::non-existing-article::", "::property::")
    }

    @Test
    fun `Get article titles by links`() {
        val articleTitles = articles.getArticleTitles(setOf("article-title"))

        assertThat(articleTitles, Is(listOf("Article title")))
    }

    @Test
    fun `Skip non-existing links when gathering full titles`() {
        val articleTitles = articles.getArticleTitles(setOf(
                "not-found-link-1", "article-title", "not-found-link-2"))

        assertThat(articleTitles, Is(listOf("Article title")))
    }

    @Test
    fun `Return empty list when link titles don't point to existing articles`() {
        val articleTitles = articles.getArticleTitles(setOf("non-existent-link-1",
                "non-existent-link-2"))

        assertThat(articleTitles, Is(emptyList()))
    }

    private fun Mockery.expecting(block: Expectations.() -> Unit) {
        checking(Expectations().apply(block))
    }
}