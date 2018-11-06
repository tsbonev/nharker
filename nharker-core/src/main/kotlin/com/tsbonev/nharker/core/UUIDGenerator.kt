package com.tsbonev.nharker.core

import java.util.UUID

/**
 * Returns an unique id, relying that UUID is random enough.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class UUIDGenerator : IdGenerator {
	override fun generateId(): String {
		return UUID.randomUUID().toString()
	}
}