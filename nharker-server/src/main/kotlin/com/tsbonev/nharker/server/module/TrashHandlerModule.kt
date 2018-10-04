package com.tsbonev.nharker.server.module

import com.tsbonev.nharker.adapter.nitrite.NitriteEntityTrashCollector
import com.tsbonev.nharker.core.TrashCollector
import com.tsbonev.nharker.server.controller.helpers.LoggingTrashHandler
import com.tsbonev.nharker.server.controller.helpers.TrashHandler
import org.koin.dsl.module.module

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */

val loggingTrashHandler = module {
    single<TrashHandler> { LoggingTrashHandler(get()) }
    single<TrashCollector> { NitriteEntityTrashCollector(get()) }
}