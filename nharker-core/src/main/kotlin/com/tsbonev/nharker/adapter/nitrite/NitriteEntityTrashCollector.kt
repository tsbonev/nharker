package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Entity
import com.tsbonev.nharker.core.EntityCannotBeCastException
import com.tsbonev.nharker.core.EntityNotInTrashException
import com.tsbonev.nharker.core.TrashCollector
import com.tsbonev.nharker.core.helpers.toDocument
import com.tsbonev.nharker.core.helpers.toEntity
import org.dizitart.kno2.filters.eq
import org.dizitart.no2.Nitrite
import org.dizitart.no2.NitriteCollection

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteEntityTrashCollector(
	private val nitriteDb: Nitrite,
	private val collectionName: String = "Entity_trash"
) : TrashCollector {
	private val coll: NitriteCollection
		get() = nitriteDb.getCollection(collectionName)

	override fun view(): List<Entity> {
		val docList = coll.find().toList()

		val entityList = mutableListOf<Entity>()

		docList.forEach { entityList.add(it.toEntity()) }

		return entityList
	}

	override fun trash(entity: Entity): String {
		val documentEntity = entity.toDocument()

		coll.insert(documentEntity)
		return documentEntity.get("entityId", String::class.java)
	}

	override fun <T> restore(id: String, entityClass: Class<T>): T {
		val restoredDoc = coll.find("entityId" eq id).firstOrNull()
			?: throw EntityNotInTrashException(id, entityClass)

		return try {
			val castDoc = entityClass.cast(restoredDoc.toEntity())
			coll.remove(restoredDoc)
			castDoc
		} catch (e: ClassCastException) {
			throw EntityCannotBeCastException(id, entityClass)
		}
	}

	override fun clear(): List<Entity> {
		val clearedEntityList = coll.find().toList().map {
			it.toEntity()
		}

		coll.drop()
		return clearedEntityList
	}
}