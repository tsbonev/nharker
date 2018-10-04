package com.tsbonev.nharker.server

import org.junit.After
import org.junit.Test
import org.koin.standalone.StandAloneContext.stopKoin
import org.koin.test.KoinTest
import org.koin.test.checkModules

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
class KoinDryModulesTest : KoinTest {
    @After
    fun cleanUp() {
        stopKoin()
    }

    @Test
    fun `Check Koin modules`() {
        checkModules(listOf(helloAppModule))
    }
}