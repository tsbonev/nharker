package com.tsbonev.nharker.server

import com.tsbonev.nharker.server.module.fakeNitrite
import com.tsbonev.nharker.server.module.loggingTrashHandler
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.junit.Before
import org.junit.Test
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.test.AutoCloseKoinTest
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
class EndToEndAppTest : AutoCloseKoinTest() {

    @Before
    fun setUp() {
        startKoin(listOf(fakeNitrite, loggingTrashHandler))
    }

    @Test
    fun `Hello route returns ktor & koin`() = withTestApplication(Application::main) {
        with(handleRequest(HttpMethod.Get, "/hello")) {
            assertThat(response.status(), Is(HttpStatusCode.OK))
            assertThat(response.content, Is("Hello, world!"))
        }
    }

    @Test
    fun `Route is not found`() = withTestApplication(Application::main) {
        with(handleRequest(HttpMethod.Get, "/index.html")) {
            assertThat(requestHandled, Is(false))
        }
    }
}