package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleLinks
import com.tsbonev.nharker.core.ArticleNotFoundException
import com.tsbonev.nharker.core.ArticleProperties
import com.tsbonev.nharker.core.ArticleRequest
import com.tsbonev.nharker.core.ArticleTitleTakenException
import com.tsbonev.nharker.core.Articles
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryAlreadyInArticleException
import com.tsbonev.nharker.core.EntryNotInArticleException
import com.tsbonev.nharker.core.PropertyNotFoundException
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
import java.util.Optional
import org.hamcrest.CoreMatchers.`is` as Is

@Suppress("UNCHECKED_CAST")
/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class ArticleWorkflowTest {

    @Rule
    @JvmField
    val context: JUnitRuleMockery = JUnitRuleMockery()

    private val eventBus = context.mock(EventBus::class.java)
    private val articles = context.mock(Articles::class.java)

    private val exceptionLogger = ExceptionLogger()

    private val articleWorkflow = ArticleWorkflow(eventBus, articles, exceptionLogger)

    private val articleRequest = ArticleRequest(
            "Full title",
            listOf("::catalogue-id::")
    )

    private val entry = Entry(
            "::entry-id::",
            LocalDateTime.now(),
            "::id::",
            "::content::"
    )

    private val propertyEntry = Entry(
            "::property-id::",
            LocalDateTime.now(),
            "::article-id::",
            "::content::"
    )
    private val article = Article(
            "::article-id::",
            "article-title",
            "Article title",
            LocalDateTime.now(),
            catalogues = setOf("::catalogue-id::"),
            entries = mapOf("::entry-id::" to 0),
            properties = ArticleProperties(mutableMapOf("::property::" to propertyEntry))
    )

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
            oneOf(articles).delete(article.id)
            will(returnValue(article))

            oneOf(eventBus).send(DeleteEntryCommand("::property-id::"))
            oneOf(eventBus).send(DeleteEntryCommand("::entry-id::"))

            oneOf(eventBus).publish(ArticleDeletedEvent(article))
        }

        val response = articleWorkflow.deleteArticle(DeleteArticleCommand(article.id))

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Article, Is(article))
    }

    @Test
    fun `Deleting a non-existent article returns not found`() {
        context.expecting {
            oneOf(articles).delete(article.id)
            will(throwException(ArticleNotFoundException(article.id)))
        }

        val response = articleWorkflow.deleteArticle(DeleteArticleCommand(article.id))

        assertThat(response.statusCode, Is(StatusCode.NotFound))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Retrieve article by id`() {
        context.expecting {
            oneOf(articles).getById(article.id)
            will(returnValue(Optional.of(article)))
        }

        val response = articleWorkflow.getArticleById(GetArticleByIdCommand(article.id))

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Article, Is(article))
    }

    @Test
    fun `Retrieving non-existent article returns not found`() {
        context.expecting {
            oneOf(articles).getById(article.id)
            will(returnValue(Optional.empty<Article>()))
        }

        val response = articleWorkflow.getArticleById(GetArticleByIdCommand(article.id))

        assertThat(response.statusCode, Is(StatusCode.NotFound))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Retrieve article by link title`() {
        context.expecting {
            oneOf(articles).getByLinkTitle(article.linkTitle)
            will(returnValue(Optional.of(article)))
        }

        val response = articleWorkflow
                .getArticleByLinkTitle(GetArticleByLinkTitleCommand(article.linkTitle))

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Article, Is(article))
    }

    @Test
    fun `Retrieving article by non-existing link title returns not found`() {
        context.expecting {
            oneOf(articles).getByLinkTitle(article.linkTitle)
            will(returnValue(Optional.empty<Article>()))
        }

        val response = articleWorkflow
                .getArticleByLinkTitle(GetArticleByLinkTitleCommand(article.linkTitle))

        assertThat(response.statusCode, Is(StatusCode.NotFound))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Search for articles by title`() {
        context.expecting {
            oneOf(articles).searchByFullTitle(article.fullTitle)
            will(returnValue(listOf(article)))
        }

        val response = articleWorkflow
                .searchArticlesByTitle(SearchArticleByTitleCommand(article.fullTitle))

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as List<Article>, Is(listOf(article)))
    }

    @Test
    fun `Append entry to article`() {
        context.expecting {
            oneOf(articles).appendEntry(article.id, propertyEntry)
            will(returnValue(article))

            oneOf(eventBus).publish(ArticleUpdatedEvent(article))
        }

        val response = articleWorkflow.appendEntryToArticle(
                AppendEntryToArticleCommand(propertyEntry, article.id))

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
                AppendEntryToArticleCommand(propertyEntry, article.id))

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
                AppendEntryToArticleCommand(propertyEntry, article.id))

        assertThat(response.statusCode, Is(StatusCode.BadRequest))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Remove entry from article`() {
        context.expecting {
            oneOf(articles).removeEntry(article.id, propertyEntry)
            will(returnValue(article))

            oneOf(eventBus).send(DeleteEntryCommand(propertyEntry.id))

            oneOf(eventBus).publish(ArticleUpdatedEvent(article))
        }

        val response = articleWorkflow.removeEntryFromArticle(
                RemoveEntryFromArticleCommand(propertyEntry, article.id))

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
                RemoveEntryFromArticleCommand(propertyEntry, article.id))

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
                RemoveEntryFromArticleCommand(propertyEntry, article.id))

        assertThat(response.statusCode, Is(StatusCode.BadRequest))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Attach property to article`() {
        context.expecting {
            oneOf(articles).attachProperty(article.id, "::property-name::", propertyEntry)
            will(returnValue(article))

            oneOf(eventBus).publish(ArticleUpdatedEvent(article))
        }

        val response = articleWorkflow.attachPropertyToArticle(
                AttachPropertyToArticleCommand("::property-name::",
                        propertyEntry,
                        article.id))

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Article, Is(article))
    }

    @Test
    fun `Attaching property to non-existing article returns not found`() {
        context.expecting {
            oneOf(articles).attachProperty(article.id, "::property-name::", propertyEntry)
            will(throwException(ArticleNotFoundException(article.id)))
        }

        val response = articleWorkflow.attachPropertyToArticle(
                AttachPropertyToArticleCommand("::property-name::",
                        propertyEntry,
                        article.id))

        assertThat(response.statusCode, Is(StatusCode.NotFound))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Detach property from article`() {
        context.expecting {
            oneOf(articles).detachProperty(article.id, "::property-name::")
            will(returnValue(article))

            oneOf(eventBus).publish(ArticleUpdatedEvent(article))
        }

        val response = articleWorkflow.detachPropertyFromArticle(
                DetachPropertyFromArticleCommand("::property-name::",
                        article.id))

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Article, Is(article))
    }

    @Test
    fun `Detaching property from non-existing article returns not found`() {
        context.expecting {
            oneOf(articles).detachProperty(article.id, "::property-name::")
            will(throwException(ArticleNotFoundException(article.id)))
        }

        val response = articleWorkflow.detachPropertyFromArticle(
                DetachPropertyFromArticleCommand("::property-name::",
                        article.id))

        assertThat(response.statusCode, Is(StatusCode.NotFound))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Detaching non-existing property from article returns bad request`() {
        val propertyName = "::property-name::"

        context.expecting {
            oneOf(articles).detachProperty(article.id, propertyName)
            will(throwException(PropertyNotFoundException(propertyName)))
        }

        val response = articleWorkflow.detachPropertyFromArticle(
                DetachPropertyFromArticleCommand(propertyName,
                        article.id))

        assertThat(response.statusCode, Is(StatusCode.BadRequest))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Retrieve article full titles by link titles`() {
        context.expecting {
            oneOf(articles).getArticleTitles(setOf(article.linkTitle))
            will(returnValue(listOf(article.fullTitle)))
        }

        val response = articleWorkflow.retrieveFullTitles(
                RetrieveFullTitlesCommand(setOf(article.linkTitle)))

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as List<String>, Is(listOf(article.fullTitle)))
    }

    @Test
    fun `Switch entries' order in article`() {
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
    fun `Switching entries' order in article that doesn't contain both returns bad request`() {
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
    fun `Save article when completely restored`() {
        context.expecting {
            //Restoring the entries
            oneOf(eventBus).send(GetEntryByIdCommand(entry.id))
            will(returnValue(CommandResponse(StatusCode.OK, entry)))

            //Restoring the properties
            oneOf(eventBus).send(GetEntryByIdCommand(propertyEntry.id))
            will(returnValue(CommandResponse(StatusCode.OK, propertyEntry)))

            //Restoring the links
            oneOf(eventBus).send(GetEntryByIdCommand(entry.id))
            will(returnValue(CommandResponse(StatusCode.OK, entry)))

            oneOf(articles).save(article)
        }

        articleWorkflow.onArticleRestored(EntityRestoredEvent(article, Article::class.java))
    }

    @Test
    fun `Restoring article restores its deleted references and relinks itself`(){
        val entryWithLinks = entry.copy(
                links = mapOf("::phrase::" to "::link::")
        )

        val propertyWithLinks = propertyEntry.copy(
                links = mapOf("::phrase::" to "::link::")
        )

        context.expecting {
            //Restoring the entries
            oneOf(eventBus).send(GetEntryByIdCommand(entry.id))
            will(returnValue(CommandResponse(StatusCode.NotFound)))

            oneOf(eventBus).send(RestoreTrashedEntityCommand(entry.id, Entry::class.java))
            will(returnValue(CommandResponse(StatusCode.OK, entryWithLinks)))

            //Restoring the properties
            oneOf(eventBus).send(GetEntryByIdCommand(propertyEntry.id))
            will(returnValue(CommandResponse(StatusCode.NotFound)))

            oneOf(eventBus).send(RestoreTrashedEntityCommand(propertyEntry.id, Entry::class.java))
            will(returnValue(CommandResponse(StatusCode.OK, propertyWithLinks)))

            //Restoring the links
            oneOf(eventBus).send(GetEntryByIdCommand(entry.id))
            will(returnValue(CommandResponse(StatusCode.OK, entryWithLinks)))

            oneOf(articles).save(article.copy(
                    properties = ArticleProperties(mutableMapOf("::property::" to propertyWithLinks)),
                    links = ArticleLinks(mutableMapOf("::link::" to 2))
            ))
        }

        articleWorkflow.onArticleRestored(EntityRestoredEvent(article.copy(), Article::class.java))
    }

    @Test
    fun `Restoring article ignores missing references`(){
        context.expecting {
            //Restoring the entries
            oneOf(eventBus).send(GetEntryByIdCommand(entry.id))
            will(returnValue(CommandResponse(StatusCode.NotFound)))

            oneOf(eventBus).send(RestoreTrashedEntityCommand(entry.id, Entry::class.java))
            will(returnValue(CommandResponse(StatusCode.NotFound)))

            //Restoring the properties
            oneOf(eventBus).send(GetEntryByIdCommand(propertyEntry.id))
            will(returnValue(CommandResponse(StatusCode.NotFound)))

            oneOf(eventBus).send(RestoreTrashedEntityCommand(propertyEntry.id, Entry::class.java))
            will(returnValue(CommandResponse(StatusCode.NotFound)))

            oneOf(articles).save(article.copy(
                    entries = emptyMap(),
                    properties = ArticleProperties(),
                    links = ArticleLinks(mutableMapOf())
            ))
        }

        articleWorkflow.onArticleRestored(EntityRestoredEvent(article, Article::class.java))
    }

    @Test
    fun `Ignore foreign restored entities`() {
        context.expecting {
            never(articles).save(article)
        }

        articleWorkflow.onArticleRestored(EntityRestoredEvent(article, String::class.java))
    }

    private fun Mockery.expecting(block: Expectations.() -> Unit) {
        checking(Expectations().apply(block))
    }
}