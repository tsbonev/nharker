package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleNotFoundException
import com.tsbonev.nharker.core.ArticlePaginationException
import com.tsbonev.nharker.core.ArticleProperties
import com.tsbonev.nharker.core.ArticleRequest
import com.tsbonev.nharker.core.ArticleTitleTakenException
import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryAlreadyInArticleException
import com.tsbonev.nharker.core.EntryNotInArticleException
import com.tsbonev.nharker.core.OrderedReferenceMap
import com.tsbonev.nharker.core.PropertyNotFoundException
import com.tsbonev.nharker.core.SortBy
import com.tsbonev.nharker.core.helpers.StubClock
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.filters.text
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
class NitriteArticlesTest {
	private val db = nitrite { }

	private val date = LocalDateTime.ofInstant(Instant.ofEpochSecond(1), ZoneOffset.UTC)
	private val stubClock = StubClock()
	private val collectionName = "Test_articles"

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
		setOf("::catalogue-id::")
	)

	private val article = Article(
		"::article-id::",
		"Article title",
		date,
		properties = ArticleProperties(mutableMapOf("::property-name::" to articleProperty.id)),
		entries = OrderedReferenceMap(
			linkedMapOf(
				firstPresavedEntry.id to 0,
				secondPresavedEntry.id to 1
			)
		),
		catalogues = setOf("::catalogue-id::")
	)

	private val catalogue = Catalogue(
		"::catalogue-id::",
		"::catalogue-title::",
		date
	)

	private val presavedArticle: Article
		get() = db.getRepository(collectionName, Article::class.java)
			.find(Article::id eq article.id).first()

	private val articles = NitriteArticles(
		nitriteDb = db,
		collectionName = collectionName,
		clock = stubClock
	)

	@Before
	fun setUp() {
		db.getRepository(collectionName, Article::class.java).insert(article)
	}

	@Test
	fun `Creating article returns it`() {
		removePresavedArticle()

		val createdArticle = articles.create(articleRequest)

		assertThat(
			createdArticle.copy(
				id = "::article-id::",
				entries = article.entries,
				catalogues = article.catalogues
			),
			Is(article.copy(properties = ArticleProperties(mutableMapOf())))
		)
	}

	@Test
	fun `Saving article returns it`() {
		removePresavedArticle()

		val savedArticle = articles.save(article)

		assertThat(savedArticle, Is(presavedArticle))
	}

	@Test(expected = ArticleTitleTakenException::class)
	fun `Creating an article with a taken title throws exception`() {
		articles.create(articleRequest)
	}

	@Test
	fun `Retrieves article by id`() {
		assertThat(articles.getById(article.id).isPresent, Is(true))
		assertThat(articles.getById(article.id).get(), Is(article))
	}

	@Test
	fun `Returns empty when article isn't found by id`() {
		assertThat(articles.getById("::fake-article-id::").isPresent, Is(false))
	}

	@Test
	fun `Retrieves all articles`() {
		assertThat(articles.getAll(SortBy.ASCENDING), Is(listOf(presavedArticle)))
		assertThat(articles.getAll(SortBy.DESCENDING), Is(listOf(presavedArticle)))
	}

	@Test
	fun `Retrieves all articles, paginated`() {
		assertThat(articles.getPaginated(SortBy.ASCENDING, 1, 1), Is(listOf(presavedArticle)))
		assertThat(articles.getPaginated(SortBy.DESCENDING, 1, 1), Is(listOf(presavedArticle)))
		assertThat(articles.getPaginated(SortBy.ASCENDING, 2, 3), Is(emptyList()))
		assertThat(articles.getPaginated(SortBy.DESCENDING, 2, 3), Is(emptyList()))
	}

	@Test
	fun `Paginating fills up larger than content page sizes`() {
		assertThat(
			articles.getPaginated(SortBy.ASCENDING, 1, 100),
			Is(listOf(presavedArticle))
		)
	}

	@Test
	fun `Paginating returns empty when pages overextend`() {
		assertThat(
			articles.getPaginated(SortBy.ASCENDING, 2, 100),
			Is(emptyList())
		)
	}

	@Test(expected = ArticlePaginationException::class)
	fun `Paginating with size less than zero throws exception`() {
		articles.getPaginated(SortBy.ASCENDING, 1, -1)
	}

	@Test(expected = ArticlePaginationException::class)
	fun `Paginating with page less than one throws exception`() {
		articles.getPaginated(SortBy.ASCENDING, 0, 2)
	}

	@Test
	fun `Changes article title`() {
		val newFullTitle = "New title"

		val updatedArticle = articles.changeTitle(article.id, newFullTitle)

		assertThat(presavedArticle, Is(updatedArticle))
		assertThat(presavedArticle.title, Is(newFullTitle))
	}

	@Test(expected = ArticleNotFoundException::class)
	fun `Changing title of non-existing article throws exception`() {
		articles.changeTitle("::non-existing-article::", "New title")
	}

	@Test(expected = ArticleTitleTakenException::class)
	fun `Changing title of article to a taken one throws exception`() {
		articles.changeTitle(article.id, article.title)
	}

	@Test
	fun `Deleting an article returns it`() {
		val deletedArticle = articles.deleteById(article.id)

		assertThat(deletedArticle, Is(article))
		assertThat(
			db.getRepository(collectionName, Article::class.java)
				.find(Article::id eq article.id).firstOrNull(), Is(nullValue())
		)
	}

	@Test(expected = ArticleNotFoundException::class)
	fun `Deleting non-existing article throws exception`() {
		articles.deleteById("::fake-article-id::")
	}

	@Test
	fun `Retrieves article by catalogue`() {
		val articleList = articles.getByCatalogue(catalogue)

		assertThat(articleList, Is(listOf(presavedArticle)))
	}

	@Test
	fun `Returns empty when no articles match the catalogue`() {
		val articleList = articles.getByCatalogue(catalogue.copy(id = "::non-referenced-id::"))

		assertThat(articleList, Is(emptyList()))
	}

	@Test
	fun `Adds catalogue to article`() {
		val updatedArticle = articles.addCatalogue(article.id, catalogue)

		assertThat(presavedArticle, Is(updatedArticle))
	}

	@Test(expected = ArticleNotFoundException::class)
	fun `Adding catalogue to non-existing article throws exception`() {
		articles.addCatalogue("::non-existing-article-id::", catalogue)
	}

	@Test
	fun `Removes catalogue from article`() {
		val updatedArticle = articles.removeCatalogue(article.id, catalogue)

		assertThat(presavedArticle, Is(updatedArticle))
	}

	@Test(expected = ArticleNotFoundException::class)
	fun `Removing catalogue to non-existing article throws exception`() {
		articles.removeCatalogue("::non-existing-article-id::", catalogue)
	}

	@Test
	fun `Appends entry to article`() {
		val updatedArticle = articles.appendEntry(article.id, entry)

		assertThat(presavedArticle, Is(updatedArticle))
	}

	@Test(expected = EntryAlreadyInArticleException::class)
	fun `Appending entry that is already in article throws exception`() {
		articles.appendEntry(article.id, firstPresavedEntry)
	}

	@Test
	fun `Removing entry from article return it`() {
		val updatedArticle = articles.removeEntry(article.id, secondPresavedEntry)

		assertThat(presavedArticle, Is(updatedArticle))
	}

	@Test
	fun `Removing entry reorders entries after deletion`() {
		articles.removeEntry(article.id, firstPresavedEntry)

		assertThat(presavedArticle.entries.raw().count(), Is(1))
		assertThat(presavedArticle.entries.raw()[secondPresavedEntry.id], Is(0))
	}

	@Test
	fun `Retrieves list of articles by querying titles`() {
		val articleList = articles.searchByFullTitle(article.title)

		assertThat(articleList, Is(listOf(article)))
	}

	@Test
	fun `Returns empty list when no articles match title search`() {
		val articleList = articles.searchByFullTitle("Non existing")

		assertThat(articleList, Is(emptyList()))
	}

	@Test(expected = ArticleNotFoundException::class)
	fun `Deleting from a non-existing article throws exception`() {
		articles.removeEntry("::fake-article-id::", firstPresavedEntry)
	}

	@Test(expected = EntryNotInArticleException::class)
	fun `Deleting an entry that is not in the article throws exception`() {
		articles.removeEntry(article.id, entry)
	}

	@Test
	fun `Switches entries' order in an article`() {
		val updatedArticle = articles.switchEntries(article.id, firstPresavedEntry, secondPresavedEntry)

		assertThat(
			updatedArticle.entries.raw(), Is(
				mapOf(
					secondPresavedEntry.id to 0,
					firstPresavedEntry.id to 1
				)
			)
		)
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
	fun `Attaches property to article`() {
		val updatedArticle = articles.attachProperty(article.id, "::propertyName::", entry)

		assertThat(
			presavedArticle,
			Is(updatedArticle)
		)
	}

	@Test(expected = ArticleNotFoundException::class)
	fun `Attaching property to non-existing article throws exception`() {
		articles.attachProperty("::non-existing-article::", "::propertyName::", entry)
	}

	@Test
	fun `Detaches property from article`() {
		val updatedArticle = articles.detachProperty(article.id, "::property-name::")

		assertThat(presavedArticle, Is(updatedArticle))
	}

	@Test(expected = PropertyNotFoundException::class)
	fun `Detaching non-existing property throws exception`() {
		articles.detachProperty(article.id, "::non-existing-property-name::")
	}

	@Test(expected = ArticleNotFoundException::class)
	fun `Detaching property from non-existing article throws exception`() {
		articles.detachProperty("::non-existing-article::", "::property-name::")
	}

	/**
	 * Removes the presaved article from the collection.
	 */
	private fun removePresavedArticle() {
		db.getRepository(collectionName, Article::class.java)
			.remove(Article::title text article.title)
	}
}