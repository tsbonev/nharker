package com.tsbonev.nharker.core.helpers

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * A stub clock that always returns the first epoch second LocalDateTime.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class StubClock(val instant: Instant = Instant.ofEpochSecond(1)) : Clock() {

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