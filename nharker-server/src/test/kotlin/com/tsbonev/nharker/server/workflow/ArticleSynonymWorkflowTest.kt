@file:Suppress("UNCHECKED_CAST")

package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleSynonymProvider
import com.tsbonev.nharker.core.SynonymAlreadyTakenException
import com.tsbonev.nharker.core.SynonymNotFoundException
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
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class ArticleSynonymWorkflowTest {
    @Rule
    @JvmField
    val context: JUnitRuleMockery = JUnitRuleMockery()

    private val date = LocalDateTime.ofEpochSecond(1, 1, ZoneOffset.UTC)

    private val article = Article(
            "::article-id::",
            "article-title",
            "Article title",
            date
    )

    private val eventBus = context.mock(EventBus::class.java)
    private val synonymProvider = context.mock(ArticleSynonymProvider::class.java)

    private val exceptionLogger = ExceptionLogger()

    private val synonym = "::synonym::"
    private val synonymMap = mapOf(synonym to article.id)

    private val synonymWorkflow = ArticleSynonymWorkflow(eventBus, synonymProvider, exceptionLogger)

    @Test
    fun `Retrieves full global map`() {
        context.expecting {
            oneOf(synonymProvider).getSynonymMap()
            will(returnValue(synonymMap))
        }

        val response = synonymWorkflow.getSynonymMap(GetSynonymMapQuery())

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Map<String, String>, Is(synonymMap))
    }

    @Test
    fun `Searches synonym map`() {
        context.expecting {
            oneOf(synonymProvider).getSynonymMap()
            will(returnValue(synonymMap))
        }

        val response = synonymWorkflow.searchSynonymMap(
                SearchSynonymMapQuery(synonym))

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as String, Is(article.id))
    }

    @Test
    fun `Searching synonym map for non-existing synonym returns not found`() {
        context.expecting {
            oneOf(synonymProvider).getSynonymMap()
            will(returnValue(synonymMap))
        }

        val response = synonymWorkflow.searchSynonymMap(
                SearchSynonymMapQuery("::non-existing-synonym::"))

        assertThat(response.statusCode, Is(StatusCode.NotFound))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Retrieves article synonyms`() {
        context.expecting {
            oneOf(synonymProvider).getSynonymMap()
            will(returnValue(synonymMap))
        }

        val response = synonymWorkflow.getSynonymsForArticle(
                GetSynonymsForArticleQuery(article))

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as List<String>, Is(listOf(synonym)))
    }

    @Test
    fun `Adding synonym returns pair of synonym and article`() {
        context.expecting {
            oneOf(synonymProvider).addSynonym(synonym, article)
            will(returnValue(synonym))

            oneOf(eventBus).publish(SynonymAddedEvent(synonym, article))
        }

        val response = synonymWorkflow.addSynonym(
                AddSynonymCommand(synonym, article))

        assertThat(response.statusCode, Is(StatusCode.Created))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Pair<String, Article>, Is(Pair(synonym, article)))
    }

    @Test
    fun `Adding synonym that already exists returns bad request`() {
        context.expecting {
            oneOf(synonymProvider).addSynonym(synonym, article)
            will(throwException(SynonymAlreadyTakenException(synonym)))
        }

        val response = synonymWorkflow.addSynonym(
                AddSynonymCommand(synonym, article))

        assertThat(response.statusCode, Is(StatusCode.BadRequest))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Removing synonym returns it`() {
        context.expecting {
            oneOf(synonymProvider).removeSynonym(synonym)
            will(returnValue(synonym to article.id))

            oneOf(eventBus).publish(SynonymRemovedEvent(synonym, article.id))
        }

        val response = synonymWorkflow.removeSynonym(
                RemoveSynonymCommand(synonym))

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Pair<String, String>, Is(synonym to article.id))
    }

    @Test
    fun `Removing non-existing synonym returns not found`() {
        context.expecting {
            oneOf(synonymProvider).removeSynonym(synonym)
            will(throwException(SynonymNotFoundException(synonym)))
        }

        val response = synonymWorkflow.removeSynonym(
                RemoveSynonymCommand(synonym))

        assertThat(response.statusCode, Is(StatusCode.NotFound))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Deleting articles removes synonyms`() {
        val firstSynonym = "::first-synonym::"
        val secondSynonym = "::second-synonym::"

        val synonymMap = mapOf(firstSynonym to article.id,
                secondSynonym to article.id)

        context.expecting {
            oneOf(synonymProvider).getSynonymMap()
            will(returnValue(synonymMap))

            oneOf(synonymProvider).removeSynonym(firstSynonym)
            will(returnValue(firstSynonym to article.id))

            oneOf(eventBus).publish(SynonymRemovedEvent(firstSynonym, article.id))

            oneOf(synonymProvider).removeSynonym(secondSynonym)
            with(returnValue(secondSynonym to article.id))

            oneOf(eventBus).publish(SynonymRemovedEvent(secondSynonym, article.id))
        }

        synonymWorkflow.onArticleDeletedRemoveSynonyms(ArticleDeletedEvent(article))
    }

    private fun Mockery.expecting(block: Expectations.() -> Unit) {
        checking(Expectations().apply(block))
    }
}