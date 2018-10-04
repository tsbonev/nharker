package com.tsbonev.nharker.server.module

import org.dizitart.kno2.nitrite
import org.koin.dsl.module.module

/**
 * Provides the database to inject into services
 *
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */

/**
 * Fake nitrite db that gets stored in memory.
 */
val fakeNitrite = module {
    single { nitrite {} }
}