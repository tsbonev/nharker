package com.tsbonev.nharker.server.workflow

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.PaginationException
import com.tsbonev.nharker.core.Paginator
import com.tsbonev.nharker.core.SortBy
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
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
@Suppress("UNCHECKED_CAST")
class PaginationWorkflowTest {
    @Rule
    @JvmField
    val context: JUnitRuleMockery = JUnitRuleMockery()

    private val exceptionLogger = ExceptionLogger()

    private val paginatorMock = context.mock(Paginator::class.java)

    private val paginationWorkflow = PaginationWorkflow(
            mapOf(String::class.java to paginatorMock),
            exceptionLogger)

    @Test
    fun `Get all articles and all entries`() {
        context.expecting {
            oneOf(paginatorMock).getAll(SortBy.DESCENDING)
            will(returnValue(emptyList<String>()))
        }

        val response = paginationWorkflow.getAllDomainObjects(
                GetAllDomainObjectsCommand(SortBy.DESCENDING, String::class.java))

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as List<String>, Is(emptyList()))
    }

    @Test
    fun `Get paginated articles`() {
        context.expecting {
            oneOf(paginatorMock).getAll(SortBy.DESCENDING, 1, 1)
            will(returnValue(emptyList<Article>()))
        }

        val response = paginationWorkflow.getAllDomainObjectsPaginated(
                GetPaginatedDomainObjectsCommand(SortBy.DESCENDING, 1, 1, String::class.java))

        assertThat(response.statusCode, Is(StatusCode.OK))
        assertThat(response.payload.isPresent, Is(true))
        assertThat(response.payload.get() as List<String>, Is(emptyList()))
    }

    @Test
    fun `Illegal page sizes and page indexes return bad request`() {
        context.expecting {
            allowing(paginatorMock).getAll(SortBy.DESCENDING, 1, 1)
            will(throwException(PaginationException(1, 1)))
        }

        val response = paginationWorkflow.getAllDomainObjectsPaginated(
                GetPaginatedDomainObjectsCommand(SortBy.DESCENDING, 1, 1, String::class.java))

        assertThat(response.statusCode, Is(StatusCode.BadRequest))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Running get all for an unregistered paginator returns bad request`() {
        val response = paginationWorkflow.getAllDomainObjects(
                GetAllDomainObjectsCommand(SortBy.DESCENDING, Int::class.java))

        assertThat(response.statusCode, Is(StatusCode.BadRequest))
        assertThat(response.payload.isPresent, Is(false))
    }

    @Test
    fun `Running paginate for an unregistered paginator returns bad request`() {
        val response = paginationWorkflow.getAllDomainObjectsPaginated(
                GetPaginatedDomainObjectsCommand(SortBy.DESCENDING, 1, 1, Int::class.java))

        assertThat(response.statusCode, Is(StatusCode.BadRequest))
        assertThat(response.payload.isPresent, Is(false))
    }

    private fun Mockery.expecting(block: Expectations.() -> Unit) {
        checking(Expectations().apply(block))
    }
}