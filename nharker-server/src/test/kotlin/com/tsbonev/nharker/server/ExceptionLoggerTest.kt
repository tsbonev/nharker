package com.tsbonev.nharker.server

import com.tsbonev.nharker.core.ArticleNotFoundException
import com.tsbonev.nharker.server.helpers.ExceptionLogger
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class ExceptionLoggerTest {

    private val exceptionLogger = ExceptionLogger()

    private val outContent = ByteArrayOutputStream()

    @Before
    fun setUpStreams() {
        System.setOut(PrintStream(outContent))
    }

    @After
    fun restoreStreams() {
        System.setOut(System.out)
    }

    @Test
    fun `Catches stored exceptions and logs`() {
        exceptionLogger.logException(ArticleNotFoundException("::article-id::"))

        assertThat(outContent.toString().contains("ERROR ExceptionLogger - There is no article with id ::article-id::!"),
                Is(true))
    }

    @Test(expected = Exception::class)
    fun `Rethrows exception that is not stored in a case`() {
        exceptionLogger.logException(Exception())

        assertThat(outContent.toString().contains("ERROR ExceptionLogger - There is no case for an exception of type java.lang.Exception"),
                Is(true))
    }
}