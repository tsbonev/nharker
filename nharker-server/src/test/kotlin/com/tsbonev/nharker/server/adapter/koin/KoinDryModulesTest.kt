package com.tsbonev.nharker.server.adapter.koin

import org.junit.After
import org.junit.Test
import org.koin.standalone.StandAloneContext.stopKoin
import org.koin.test.KoinTest
import org.koin.test.checkModules

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class KoinDryModulesTest : KoinTest {
    @After
    fun cleanUp() {
        stopKoin()
    }

    @Test
    fun `Checks Koin modules`() {
        checkModules(listOf(fakeNitrite))
    }
}