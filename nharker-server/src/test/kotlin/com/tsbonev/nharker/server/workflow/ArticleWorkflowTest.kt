@file:Suppress("UNCHECKED_CAST")

package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleNotFoundException
import com.tsbonev.nharker.core.ArticlePaginationException
import com.tsbonev.nharker.core.ArticleProperties
import com.tsbonev.nharker.core.ArticleRequest
import com.tsbonev.nharker.core.ArticleTitleTakenException
import com.tsbonev.nharker.core.Articles
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryAlreadyInArticleException
import com.tsbonev.nharker.core.EntryNotInArticleException
import com.tsbonev.nharker.core.OrderedReferenceMap
import com.tsbonev.nharker.core.PropertyNotFoundException
import com.tsbonev.nharker.core.SortBy
import com.tsbonev.nharker.cqrs.CommandResponse
import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.cqrs.StatusCode
import com.tsbonev.nharker.server.helpers.ExceptionLogger
import org.jmock.AbstractExpectations.returnValue
import org.jmock.AbstractExpectations.throwException
import org.jmock.Expectations
import org.jmock.Mockery
import org.jmock.integration.junit4.JUnitRuleMockery
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class ArticleWorkflowTest {
	@Rule
	@JvmField
	val context: JUnitRuleMockery = JUnitRuleMockery()

	private val date = LocalDateTime.ofEpochSecond(1, 1, ZoneOffset.UTC)

	private val articleRequest = ArticleRequest(
		"Article title",
		setOf("::catalogue-id::")
	)

	private val entry = Entry(
		"::entry-id::",
		date,
		"::id::",
		"::content::"
	)

	private val propertyName = "::property-name::"
	private val propertyEntry = Entry(
		"::property-id::",
		date,
		"::article-id::",
		"::content::"
	)
	private val article = Article(
		"::article-id::",
		"Article title",
		date,
		catalogues = setOf("::catalogue-id::"),
		entries = OrderedReferenceMap(linkedMapOf("::entry-id::" to 0)),
		properties = ArticleProperties(mutableMapOf(propertyName to propertyEntry.id))
	)

	private val eventBus = context.mock(EventBus::class.java)
	private val articles = context.mock(Articles::class.java)

	private val exceptionLogger = ExceptionLogger()

	private val articleWorkflow = ArticleWorkflow(eventBus, articles, exceptionLogger)

	@Test
	fun `Creating article returns it`() {
		context.expecting {
			oneOf(articles).create(articleRequest)
			will(returnValue(article))

			oneOf(eventBus).publish(ArticleCreatedEvent(article))
		}

		val response = articleWorkflow.createArticle(CreateArticleCommand(articleRequest))

		assertThat(response.statusCode, Is(StatusCode.Created))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as Article, Is(article))
	}

	@Test
	fun `Creating article with a taken title returns bad request`() {
		context.expecting {
			oneOf(articles).create(articleRequest)
			will(throwException(ArticleTitleTakenException(articleRequest.fullTitle)))
		}

		val response = articleWorkflow.createArticle(CreateArticleCommand(articleRequest))

		assertThat(response.statusCode, Is(StatusCode.BadRequest))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Deleting an article returns it and deletes entries`() {
		context.expecting {
			oneOf(articles).deleteById(article.id)
			will(returnValue(article))

			oneOf(eventBus).send(DeleteEntryCommand(propertyEntry.id))
			oneOf(eventBus).send(DeleteEntryCommand(entry.id))

			oneOf(eventBus).publish(ArticleDeletedEvent(article))
		}

		val response = articleWorkflow.deleteArticle(DeleteArticleCommand(article.id))

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as Article, Is(article))
	}

	@Test
	fun `Deleting a non-existing article returns not found`() {
		context.expecting {
			oneOf(articles).deleteById(article.id)
			will(throwException(ArticleNotFoundException(article.id)))
		}

		val response = articleWorkflow.deleteArticle(DeleteArticleCommand(article.id))

		assertThat(response.statusCode, Is(StatusCode.NotFound))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Retrieves article by id`() {
		context.expecting {
			oneOf(articles).getById(article.id)
			will(returnValue(Optional.of(article)))
		}

		val response = articleWorkflow.getArticleById(GetArticleByIdQuery(article.id))

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as Article, Is(article))
	}

	@Test
	fun `Retrieving non-existing article returns not found`() {
		context.expecting {
			oneOf(articles).getById(article.id)
			will(returnValue(Optional.empty<Article>()))
		}

		val response = articleWorkflow.getArticleById(GetArticleByIdQuery(article.id))

		assertThat(response.statusCode, Is(StatusCode.NotFound))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Retrieves articles searching by title`() {
		context.expecting {
			oneOf(articles).searchByFullTitle(article.title)
			will(returnValue(listOf(article)))
		}

		val response = articleWorkflow
			.searchArticlesByTitle(SearchArticleByTitleQuery(article.title))

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as List<Article>, Is(listOf(article)))
	}

	@Test
	fun `Retrieves all articles`() {
		val sortOrder = SortBy.ASCENDING

		context.expecting {
			oneOf(articles).getAll(sortOrder)
			will(returnValue(listOf(article)))
		}

		val response = articleWorkflow
			.getAllArticles(GetAllArticlesQuery(sortOrder))

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as List<Article>, Is(listOf(article)))
	}

	@Test
	fun `Retrieves paginated articles`() {
		val sortOrder = SortBy.ASCENDING

		context.expecting {
			oneOf(articles).getPaginated(sortOrder, 1, 1)
			will(returnValue(listOf(article)))
		}

		val response = articleWorkflow
			.getPaginatedArticles(GetPaginatedArticlesQuery(sortOrder, 1, 1))

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as List<Article>, Is(listOf(article)))
	}

	@Test
	fun `Paginating with illegal page count and size returns bad request`() {
		val sortOrder = SortBy.ASCENDING

		context.expecting {
			oneOf(articles).getPaginated(sortOrder, -1, 0)
			will(throwException(ArticlePaginationException(-1, 0)))
		}

		val response = articleWorkflow
			.getPaginatedArticles(GetPaginatedArticlesQuery(sortOrder, -1, 0))

		assertThat(response.statusCode, Is(StatusCode.BadRequest))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Appends entry to article`() {
		context.expecting {
			oneOf(articles).appendEntry(article.id, propertyEntry)
			will(returnValue(article))

			oneOf(eventBus).publish(ArticleUpdatedEvent(article))
		}

		val response = articleWorkflow.appendEntryToArticle(
			AppendEntryToArticleCommand(propertyEntry, article.id)
		)

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as Article, Is(article))
	}

	@Test
	fun `Appending entry to non-existing article returns not found`() {
		context.expecting {
			oneOf(articles).appendEntry(article.id, propertyEntry)
			will(throwException(ArticleNotFoundException(article.id)))
		}

		val response = articleWorkflow.appendEntryToArticle(
			AppendEntryToArticleCommand(propertyEntry, article.id)
		)

		assertThat(response.statusCode, Is(StatusCode.NotFound))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Appending entry that is already in article returns bad request`() {
		context.expecting {
			oneOf(articles).appendEntry(article.id, propertyEntry)
			will(throwException(EntryAlreadyInArticleException(propertyEntry.id, article.id)))
		}

		val response = articleWorkflow.appendEntryToArticle(
			AppendEntryToArticleCommand(propertyEntry, article.id)
		)

		assertThat(response.statusCode, Is(StatusCode.BadRequest))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Removes entry from article`() {
		context.expecting {
			oneOf(articles).removeEntry(article.id, propertyEntry)
			will(returnValue(article))

			oneOf(eventBus).send(DeleteEntryCommand(propertyEntry.id))

			oneOf(eventBus).publish(ArticleUpdatedEvent(article))
		}

		val response = articleWorkflow.removeEntryFromArticle(
			RemoveEntryFromArticleCommand(propertyEntry, article.id)
		)

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as Article, Is(article))
	}

	@Test
	fun `Removing entry from non-existing article returns not found`() {
		context.expecting {
			oneOf(articles).removeEntry(article.id, propertyEntry)
			will(throwException(ArticleNotFoundException(article.id)))
		}

		val response = articleWorkflow.removeEntryFromArticle(
			RemoveEntryFromArticleCommand(propertyEntry, article.id)
		)

		assertThat(response.statusCode, Is(StatusCode.NotFound))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Removing entry that is not in article returns bad request`() {
		context.expecting {
			oneOf(articles).removeEntry(article.id, propertyEntry)
			will(throwException(EntryNotInArticleException(propertyEntry.id, article.id)))
		}

		val response = articleWorkflow.removeEntryFromArticle(
			RemoveEntryFromArticleCommand(propertyEntry, article.id)
		)

		assertThat(response.statusCode, Is(StatusCode.BadRequest))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Attaches property to article`() {
		context.expecting {
			oneOf(articles).attachProperty(article.id, propertyName, propertyEntry)
			will(returnValue(article))

			oneOf(eventBus).publish(ArticleUpdatedEvent(article))
		}

		val response = articleWorkflow.attachPropertyToArticle(
			AttachPropertyToArticleCommand(
				propertyName,
				propertyEntry,
				article.id
			)
		)

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as Article, Is(article))
	}

	@Test
	fun `Attaching property to non-existing article returns not found`() {
		context.expecting {
			oneOf(articles).attachProperty(article.id, propertyName, propertyEntry)
			will(throwException(ArticleNotFoundException(article.id)))
		}

		val response = articleWorkflow.attachPropertyToArticle(
			AttachPropertyToArticleCommand(
				propertyName,
				propertyEntry,
				article.id
			)
		)

		assertThat(response.statusCode, Is(StatusCode.NotFound))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Detaches property from article`() {
		context.expecting {
			oneOf(articles).detachProperty(article.id, propertyName)
			will(returnValue(Pair(article, propertyEntry.id)))

			oneOf(eventBus).send(DeleteEntryCommand(propertyEntry.id))

			oneOf(eventBus).publish(ArticleUpdatedEvent(article))
		}

		val response = articleWorkflow.detachPropertyFromArticle(
			DetachPropertyFromArticleCommand(
				propertyName,
				article.id
			)
		)

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as Pair<Article, String>, Is(Pair(article, propertyEntry.id)))
	}

	@Test
	fun `Detaching property from non-existing article returns not found`() {
		context.expecting {
			oneOf(articles).detachProperty(article.id, propertyName)
			will(throwException(ArticleNotFoundException(article.id)))
		}

		val response = articleWorkflow.detachPropertyFromArticle(
			DetachPropertyFromArticleCommand(
				propertyName,
				article.id
			)
		)

		assertThat(response.statusCode, Is(StatusCode.NotFound))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Detaching non-existing property from article returns bad request`() {
		context.expecting {
			oneOf(articles).detachProperty(article.id, propertyName)
			will(throwException(PropertyNotFoundException(propertyName)))
		}

		val response = articleWorkflow.detachPropertyFromArticle(
			DetachPropertyFromArticleCommand(
				propertyName,
				article.id
			)
		)

		assertThat(response.statusCode, Is(StatusCode.BadRequest))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Switches entries' order in article`() {
		context.expecting {
			oneOf(articles).switchEntries(article.id, propertyEntry, propertyEntry)
			will(returnValue(article))

			oneOf(eventBus).publish(ArticleUpdatedEvent(article))
		}

		val response = articleWorkflow.switchEntriesInArticle(
			SwitchEntriesInArticleCommand(article.id, propertyEntry, propertyEntry)
		)

		assertThat(response.statusCode, Is(StatusCode.OK))
		assertThat(response.payload.isPresent, Is(true))
		assertThat(response.payload.get() as Article, Is(article))
	}

	@Test
	fun `Switching entries' order in non-existing article returns not found`() {
		context.expecting {
			oneOf(articles).switchEntries(article.id, propertyEntry, propertyEntry)
			will(throwException(ArticleNotFoundException(article.id)))
		}

		val response = articleWorkflow.switchEntriesInArticle(
			SwitchEntriesInArticleCommand(article.id, propertyEntry, propertyEntry)
		)

		assertThat(response.statusCode, Is(StatusCode.NotFound))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Switching entries' order in article that does not contain both returns bad request`() {
		context.expecting {
			oneOf(articles).switchEntries(article.id, propertyEntry, propertyEntry)
			will(throwException(EntryNotInArticleException(propertyEntry.id, article.id)))
		}

		val response = articleWorkflow.switchEntriesInArticle(
			SwitchEntriesInArticleCommand(article.id, propertyEntry, propertyEntry)
		)

		assertThat(response.statusCode, Is(StatusCode.BadRequest))
		assertThat(response.payload.isPresent, Is(false))
	}

	@Test
	fun `Saves article when completely restored`() {
		context.expecting {
			//Restoring the entries
			oneOf(eventBus).send(GetEntryByIdQuery(entry.id))
			will(returnValue(CommandResponse(StatusCode.OK, entry)))

			//Restoring the properties
			oneOf(eventBus).send(GetEntryByIdQuery(propertyEntry.id))
			will(returnValue(CommandResponse(StatusCode.OK, propertyEntry)))

			oneOf(articles).save(article)
		}

		articleWorkflow.onArticleRestored(EntityRestoredEvent(article, Article::class.java))
	}

	@Test
	fun `Restores article's entries and properties`() {
		context.expecting {
			//Restoring the entries
			oneOf(eventBus).send(GetEntryByIdQuery(entry.id))
			will(returnValue(CommandResponse(StatusCode.NotFound)))

			oneOf(eventBus).send(RestoreTrashedEntityCommand(entry.id, Entry::class.java))
			will(returnValue(CommandResponse(StatusCode.OK, entry)))

			//Restoring the properties
			oneOf(eventBus).send(GetEntryByIdQuery(propertyEntry.id))
			will(returnValue(CommandResponse(StatusCode.NotFound)))

			oneOf(eventBus).send(RestoreTrashedEntityCommand(propertyEntry.id, Entry::class.java))
			will(returnValue(CommandResponse(StatusCode.OK, propertyEntry)))

			oneOf(articles).save(article)
		}

		articleWorkflow.onArticleRestored(EntityRestoredEvent(article, Article::class.java))
	}

	@Test
	fun `Ignores article's unrestorable entries and properties`() {
		context.expecting {
			//Restoring the entries
			oneOf(eventBus).send(GetEntryByIdQuery(entry.id))
			will(returnValue(CommandResponse(StatusCode.NotFound)))

			oneOf(eventBus).send(RestoreTrashedEntityCommand(entry.id, Entry::class.java))
			will(returnValue(CommandResponse(StatusCode.NotFound)))

			//Restoring the properties
			oneOf(eventBus).send(GetEntryByIdQuery(propertyEntry.id))
			will(returnValue(CommandResponse(StatusCode.NotFound)))

			oneOf(eventBus).send(RestoreTrashedEntityCommand(propertyEntry.id, Entry::class.java))
			will(returnValue(CommandResponse(StatusCode.NotFound)))

			oneOf(articles).save(
				article.copy(
					entries = OrderedReferenceMap(),
					properties = ArticleProperties()
				)
			)
		}

		articleWorkflow.onArticleRestored(EntityRestoredEvent(article, Article::class.java))
	}

	@Test
	fun `Ignores foreign restored entities`() {
		context.expecting {
			never(articles).save(article)
		}

		articleWorkflow.onArticleRestored(EntityRestoredEvent(article, String::class.java))
	}

	@Test
	fun `Relinks article's entries when refreshed`() {
		context.expecting {
			oneOf(eventBus).send(LinkEntryContentToArticlesCommand(entry))
		}

		articleWorkflow.onArticleRefreshed(ArticleLinksRefreshedEvent(article, listOf(entry)))
	}

	private fun Mockery.expecting(block: Expectations.() -> Unit) {
		checking(Expectations().apply(block))
	}
}