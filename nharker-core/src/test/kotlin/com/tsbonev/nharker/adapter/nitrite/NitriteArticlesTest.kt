package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleLinks
import com.tsbonev.nharker.core.ArticleNotFoundException
import com.tsbonev.nharker.core.ArticleProperties
import com.tsbonev.nharker.core.ArticleRequest
import com.tsbonev.nharker.core.ArticleTitleTakenException
import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryAlreadyInArticleException
import com.tsbonev.nharker.core.EntryLinker
import com.tsbonev.nharker.core.EntryNotInArticleException
import com.tsbonev.nharker.core.PropertyNotFoundException
import com.tsbonev.nharker.core.SortBy
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
            "::entry-id::",
            date,
            "::article-id::",
            "::content::",
            mapOf("::content::" to "::article::")
    )

    private val firstPresavedEntry = Entry(
            "::first-presaved-entry-id::",
            date,
            "::article-id::",
            "::content::",
            emptyMap()
    )

    private val secondPresavedEntry = Entry(
            "::second-presaved-entry-id::",
            date,
            "::article-id::",
            "::content::",
            emptyMap()
    )

    private val articleProperty = Entry(
            "::article-property-id::",
            date,
            "::article-id::",
            "::content::",
            emptyMap()
    )

    private val articleRequest = ArticleRequest(
            "Article title",
            listOf("::catalogue-id::")
    )

    private val article = Article(
            "::article-id::",
            "article-title",
            "Article title",
            date,
            properties = ArticleProperties(mutableMapOf("::property-name::" to articleProperty)),
            entries = mapOf(firstPresavedEntry.id to 0,
                    secondPresavedEntry.id to 1),
            links = ArticleLinks(mutableMapOf("article-title-1" to 2,
                    "article-title-2" to 1)),
            catalogues = setOf("::catalogue-id::")
    )

    private val catalogue = Catalogue(
            "::catalogue-id::",
            "::title::",
            date
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

        assertThat(createdArticle.copy(id = "::article-id::",
                entries = article.entries,
                links = article.links,
                catalogues = article.catalogues),
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
        assertThat(articles.getById("::fake-article-id::").isPresent, Is(false))
    }

    @Test
    fun `Retrieve article by link title`() {
        assertThat(articles.getByLinkTitle(article.linkTitle).isPresent, Is(true))
        assertThat(articles.getByLinkTitle(article.linkTitle).get(), Is(article))
    }

    @Test
    fun `Return empty when article isn't found by link title`() {
        assertThat(articles.getByLinkTitle("::non-existing-link-title::").isPresent, Is(false))
    }

    @Test
    fun `Retrieve all articles`() {
        assertThat(articles.getAll(SortBy.ASCENDING), Is(listOf(presavedArticle)))
    }

    @Test
    fun `Retrieve all articles, paginated`() {
        assertThat(articles.getAll(SortBy.ASCENDING, 1, 1), Is(listOf(presavedArticle)))
        assertThat(articles.getAll(SortBy.ASCENDING, 2, 3), Is(emptyList()))
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
    fun `Retrieve article by catalogue`(){
        val articleList = articles.getByCatalogue(catalogue)

        assertThat(articleList, Is(listOf(presavedArticle)))
    }

    @Test
    fun `Return empty when no articles match the catalogue`(){
        val articleList = articles.getByCatalogue(catalogue.copy(id = "::non-referenced-id::"))

        assertThat(articleList, Is(emptyList()))
    }

    @Test
    fun `Add catalogue to article`(){
        val updatedArticle = articles.addCatalogue(article.id, catalogue)

        assertThat(presavedArticle, Is(updatedArticle))
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Adding catalogue to non-existent article throws exception`(){
        articles.addCatalogue("::non-existent-article-id::", catalogue)
    }

    @Test
    fun `Remove catalogue from article`(){
        val updatedArticle = articles.removeCatalogue(article.id, catalogue)

        assertThat(presavedArticle, Is(updatedArticle))
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Removing catalogue to non-existent article throws exception`(){
        articles.removeCatalogue("::non-existent-article-id::", catalogue)
    }

    @Test
    fun `Append entry to article`() {
        context.expecting {
            oneOf(entryLinker).findArticleLinks(entry, listOf("article-title"))
            will(returnValue(emptySet<String>()))
        }

        val updatedArticle = articles.appendEntry(article.id, entry)

        assertThat(presavedArticle, Is(updatedArticle))
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

        val updatedArticle = articles.removeEntry(article.id, secondPresavedEntry)

        assertThat(presavedArticle, Is(updatedArticle))
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
    fun `Retrieve list of articles by querying full titles`() {
        val articleList = articles.searchByFullTitle(article.fullTitle)

        assertThat(articleList, Is(listOf(article)))
    }

    @Test
    fun `Return empty list when no articles match full title search`() {
        val articleList = articles.searchByFullTitle("Non existent")

        assertThat(articleList, Is(emptyList()))
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Deleting from a non-existent article throws exception`() {
        articles.removeEntry("::fake-article-id::", firstPresavedEntry)
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
        articles.switchEntries("::fake-article-id::", firstPresavedEntry, secondPresavedEntry)
    }

    @Test(expected = EntryNotInArticleException::class)
    fun `Switching entries of non-containing article throws exception`() {
        articles.switchEntries(article.id, entry, secondPresavedEntry)
    }

    @Test
    fun `Attach property to article`() {
        val updatedArticle = articles.attachProperty(article.id, "::propertyName::", entry)

        assertThat(presavedArticle,
                Is(updatedArticle))
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Attaching property to non-existent article throws exception`() {
        articles.attachProperty("::non-existing-article::", "::propertyName::", entry)
    }

    @Test
    fun `Detach property from article`() {
        val updatedArticle = articles.detachProperty(article.id, "::property-name::")

        assertThat(presavedArticle, Is(updatedArticle))
    }

    @Test(expected = PropertyNotFoundException::class)
    fun `Detaching non-existent property throws exception`() {
        articles.detachProperty(article.id, "::non-existent-property-name::")
    }

    @Test(expected = ArticleNotFoundException::class)
    fun `Detaching property from non-existent article throws exception`() {
        articles.detachProperty("::non-existing-article::", "::property-name::")
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