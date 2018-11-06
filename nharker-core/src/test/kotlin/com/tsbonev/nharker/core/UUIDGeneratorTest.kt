package com.tsbonev.nharker.core

import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat
import org.junit.Test

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class UUIDGeneratorTest {

	private val idGenerator = UUIDGenerator()

	@Test
	fun `Generates two unique ids`(){
		val firstId = idGenerator.generateId()
		val secondId = idGenerator.generateId()

		assertThat(firstId, Is(not(secondId)))
	}
}