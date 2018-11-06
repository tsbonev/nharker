package com.tsbonev.nharker.server.end2end

import com.tsbonev.nharker.server.adapter.koin.simpleCqrsModule
import com.tsbonev.nharker.server.adapter.koin.exceptionLoggingModule
import com.tsbonev.nharker.server.adapter.koin.fakeNitriteDbModule
import com.tsbonev.nharker.server.adapter.koin.nitriteEntryLinkerModule
import com.tsbonev.nharker.server.adapter.koin.nitritePersistenceModule
import com.tsbonev.nharker.server.adapter.koin.workflowModule
import org.junit.rules.ExternalResource
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.standalone.StandAloneContext.stopKoin

/**
 * Rule that sets up the koin context and closes it during testing.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class KoinRule : ExternalResource() {
	private val moduleList = listOf(
		fakeNitriteDbModule,
		nitritePersistenceModule,
		nitriteEntryLinkerModule,
		simpleCqrsModule,
		exceptionLoggingModule,
		workflowModule
	)

	override fun before() {
		startKoin(moduleList)
	}

	override fun after() {
		stopKoin()
	}
}