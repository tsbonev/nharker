package com.tsbonev.nharker.server

import com.tsbonev.nharker.core.ArticleNotFoundException
import com.tsbonev.nharker.cqrs.StatusCode
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
	private val outContent = ByteArrayOutputStream()

	private val exceptionLogger = ExceptionLogger()

	@Before
	fun setUpStreams() {
		System.setOut(PrintStream(outContent))
	}

	@After
	fun restoreStreams() {
		System.setOut(System.out)
	}

	@Test
	fun `Catches stored exceptions, logs and returns command response`() {
		val response = exceptionLogger.logException(ArticleNotFoundException("::article-id::"))

		assertThat(
			outContent.toString().contains("ERROR ExceptionLogger - There is no article with id ::article-id::!"),
			Is(true)
		)
		assertThat(response.statusCode, Is(StatusCode.NotFound))
	}

	@Test
	fun `Logs exception that is not stored in a case and returns internal server error`() {
		val response = exceptionLogger.logException(Exception())

		assertThat(
			outContent.toString().contains("ERROR ExceptionLogger - There is no case for an exception of type java.lang.Exception"),
			Is(true)
		)
		assertThat(response.statusCode, Is(StatusCode.InternalServerError))
	}
}