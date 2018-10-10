package com.tsbonev.nharker.core.helpers

import org.junit.Assert.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class StubClockTest {

    private val instant = Instant.ofEpochSecond(15)
    private val stubClock = StubClock(instant)

    @Test
    fun `Instant of clock is stubbed`() {
        assertThat(stubClock.instant, Is(instant))
    }

    @Test
    fun `Zone of clock is UTC`() {
        assertThat(stubClock.zone, Is(ZoneId.of("Z")))
    }

    @Test
    fun `Zone of clock cannot change`() {
        assertThat(stubClock.withZone(ZoneId.of("GMT")).zone, Is(ZoneId.of("Z")))
    }

    @Test
    fun `Clock time is fixed`() {
        assertThat(LocalDateTime.ofInstant(instant, ZoneOffset.UTC),
                Is(LocalDateTime.now(stubClock)))
    }
}