package com.tsbonev.nharker.server

import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.server.adapter.koin.exceptionLoggingModule
import com.tsbonev.nharker.server.adapter.koin.fakeNitriteDbModule
import com.tsbonev.nharker.server.adapter.koin.nitriteEntryLinkerModule
import com.tsbonev.nharker.server.adapter.koin.nitritePersistenceModule
import com.tsbonev.nharker.server.adapter.koin.simpleCqrsModule
import com.tsbonev.nharker.server.adapter.koin.workflowModule
import com.tsbonev.nharker.server.adapter.ktor.catalogueRoute
import com.tsbonev.nharker.server.workflow.CatalogueWorkflow
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.routing.routing
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.ktor.ext.inject
import org.koin.standalone.StandAloneContext.startKoin

/**
 * Main configuration file for the application.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */

fun Application.main() {
	install(DefaultHeaders)
	install(CallLogging)
	install(ContentNegotiation) {
		gson {
			setPrettyPrinting()

			disableHtmlEscaping()
			enableComplexMapKeySerialization()

			serializeNulls()
		}
	}

	val eventBus: EventBus by inject()

	val catalogueWorkflow: CatalogueWorkflow by inject()

	with(eventBus) {
		registerWorkflow(catalogueWorkflow)
	}

	routing {
		catalogueRoute()
	}
}

fun main(args: Array<String>) {
	startKoin(
		listOf(
			fakeNitriteDbModule,
			nitritePersistenceModule,
			nitriteEntryLinkerModule,
			simpleCqrsModule,
			exceptionLoggingModule,
			workflowModule
		)
	)

	embeddedServer(
		factory = Netty,
		environment = commandLineEnvironment(args)
	).start(true)
}