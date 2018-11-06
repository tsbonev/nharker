@file:Suppress("UNCHECKED_CAST")

package com.tsbonev.nharker.adapter.nitrite.helpers

import com.tsbonev.nharker.core.IdGenerator
import org.dizitart.kno2.nitrite
import org.jmock.AbstractExpectations.returnValue
import org.jmock.Expectations
import org.jmock.Mockery
import org.jmock.integration.junit4.JUnitRuleMockery
import org.junit.Rule
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class UniqueNitriteIdGenerationTest {
	@Rule
	@JvmField
	val context: JUnitRuleMockery = JUnitRuleMockery()

	private val db = nitrite {  }

	data class TestEntity(val id: String)

	private val testEntity = TestEntity("::id-1::")

	private val repo = db.getRepository(TestEntity::class.java)

	private val idGeneratorOrigin = context.mock(IdGenerator::class.java)

	@Test
	fun `Regenerates id while it is not unique`(){
		repo.insert(testEntity)

		context.expecting {
			exactly(5).of(idGeneratorOrigin).generateId()
			will(returnValue("::id-1::"))

			oneOf(idGeneratorOrigin).generateId()
			will(returnValue("::id-2::"))
		}

		val generatedId = idGeneratorOrigin.generateNitriteUniqueId(repo)

		assertThat(generatedId, Is("::id-2::"))
	}

	private fun Mockery.expecting(block: Expectations.() -> Unit){
	        checking(Expectations().apply(block))
	}
}