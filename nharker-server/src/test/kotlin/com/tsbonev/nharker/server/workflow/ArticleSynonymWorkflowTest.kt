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
import org.hamcrest.CoreMatchers.`is` as Is

@Suppress("UNCHECKED_CAST")
/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class ArticleSynonymWorkflowTest {

    @Rule
    @JvmField
    val context: JUnitRuleMockery = JUnitRuleMockery()

    private val article = Article(
            "::id::",
            "link-title",
            "full-title",
            LocalDateTime.now()
    )

    private val eventBus = context.mock(EventBus::class.java)
    private val synonymProvider = context.mock(ArticleSynonymProvider::class.java)

    private val exceptionLogger = ExceptionLogger()

    private val synonymWorkflow = ArticleSynonymWorkflow(eventBus, synonymProvider, exceptionLogger)

    @Test
    fun `Retrieve full global map`() {
        val synonymMap = mapOf("::synonym::" to "::link-title::")

        context.expecting {
            oneOf(synonymProvider).getSynonymMap()
            will(returnValue(synonymMap))
        }

        val response = synonymWorkflow.getSynonymMap(GetSynonymMapCommand())

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as Map<String, String>, Is(synonymMap))
    }

    @Test
    fun `Search synonym map`() {
        val synonymMap = mapOf("::synonym::" to "::link-title::")

        context.expecting {
            oneOf(synonymProvider).getSynonymMap()
            will(returnValue(synonymMap))
        }

        val response = synonymWorkflow.searchSynonymMap(
                SearchSynonymMapCommand("::synonym::"))

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as String, Is("::link-title::"))
    }

    @Test
    fun `Searching synonym map for non-existent synonym returns not found`() {
        val synonymMap = mapOf("::synonym::" to "::link-title::")

        context.expecting {
            oneOf(synonymProvider).getSynonymMap()
            will(returnValue(synonymMap))
        }

        val response = synonymWorkflow.searchSynonymMap(
                SearchSynonymMapCommand("::non-existing-synonym::"))

        assertThat(response.statusCode, Is(StatusCode.NotFound))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Get article synonyms`() {
        val synonym = "::synonym::"
        val synonymMap = mapOf(synonym to article.linkTitle)

        context.expecting {
            oneOf(synonymProvider).getSynonymMap()
            will(returnValue(synonymMap))
        }

        val response = synonymWorkflow.getSynonymsForArticle(
                GetSynonymsForArticleCommand(article))

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as List<String>, Is(listOf(synonym)))
    }

    @Test
    fun `Adding synonym returns pair of synonym and article`() {
        val synonym = "::synonym::"

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
        val synonym = "::synonym::"

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
        val synonym = "::synonym::"

        context.expecting {
            oneOf(synonymProvider).removeSynonym(synonym)
            will(returnValue(synonym))

            oneOf(eventBus).publish(SynonymRemovedEvent(synonym))
        }

        val response = synonymWorkflow.removeSynonym(
                RemoveSynonymCommand(synonym))

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as String, Is(synonym))
    }

    @Test
    fun `Removing non-existent synonym returns not found`() {
        val synonym = "::synonym::"

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
    fun `Deleted articles get synonyms removed`() {
        val firstSynonym = "::first-synonym::"
        val secondSynonym = "::second-synonym::"

        val synonymMap = mapOf(firstSynonym to article.linkTitle,
                secondSynonym to article.linkTitle)

        context.expecting {
            oneOf(synonymProvider).getSynonymMap()
            will(returnValue(synonymMap))

            oneOf(synonymProvider).removeSynonym(firstSynonym)
            oneOf(eventBus).publish(SynonymRemovedEvent(firstSynonym))

            oneOf(synonymProvider).removeSynonym(secondSynonym)
            oneOf(eventBus).publish(SynonymRemovedEvent(secondSynonym))
        }

        synonymWorkflow.onArticleDeletedRemoveSynonyms(ArticleDeletedEvent(article))
    }

    private fun Mockery.expecting(block: Expectations.() -> Unit) {
        checking(Expectations().apply(block))
    }
}