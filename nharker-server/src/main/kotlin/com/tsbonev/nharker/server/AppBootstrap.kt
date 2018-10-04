package com.tsbonev.nharker.server

import com.tsbonev.nharker.adapter.HelloService
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.ktor.ext.inject
import org.koin.standalone.StandAloneContext.startKoin

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */

fun Application.main() {

    install(DefaultHeaders)
    install(CallLogging)

    val service: HelloService by inject()

    routing {
        get("/hello") {
            call.respondText(service.sayHello(), ContentType.Text.Plain)
        }
    }
}

fun main(args: Array<String>) {
    // Start Koin
    startKoin(listOf(helloAppModule))
    // Start Ktor
    embeddedServer(Netty, commandLineEnvironment(args)).start(true)
}