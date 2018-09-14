package com.tsbonev.nharker.core.helpers

import java.time.*

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class StubClock(val instant: Instant = Instant.ofEpochSecond(1))
    : Clock() {
    override fun withZone(p0: ZoneId?): Clock {
        return this
    }

    override fun getZone(): ZoneId {
        return ZoneOffset.UTC
    }

    override fun instant(): Instant {
        return instant
    }
}