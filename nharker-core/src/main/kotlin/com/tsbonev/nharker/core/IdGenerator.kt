package com.tsbonev.nharker.core

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface IdGenerator {
	/**
	 * Generates a database-unique id.
	 *
	 * @return The unique id.
	 */
	fun generateId() : String
}