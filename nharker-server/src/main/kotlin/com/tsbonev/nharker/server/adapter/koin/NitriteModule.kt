package com.tsbonev.nharker.server.adapter.koin

import org.dizitart.kno2.nitrite
import org.koin.dsl.module.module

/**
 * Provides the database to inject into services
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */

/**
 * Fake nitrite db that gets stored in memory.
 */
val fakeNitrite = module {
    single { nitrite {} }
}