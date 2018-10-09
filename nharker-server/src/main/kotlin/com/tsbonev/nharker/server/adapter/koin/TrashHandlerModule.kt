package com.tsbonev.nharker.server.adapter.koin

import com.tsbonev.nharker.adapter.nitrite.NitriteEntityTrashCollector
import com.tsbonev.nharker.core.TrashCollector
import com.tsbonev.nharker.server.LoggingTrashHandler
import com.tsbonev.nharker.server.TrashHandler
import org.koin.dsl.module.module

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */

val loggingTrashHandler = module {
    single<TrashHandler> { LoggingTrashHandler(get()) }
    single<TrashCollector> { NitriteEntityTrashCollector(get()) }
}