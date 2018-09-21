package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.*
import com.tsbonev.nharker.core.helpers.toDocument
import com.tsbonev.nharker.core.helpers.toEntity
import org.dizitart.kno2.filters.eq
import org.dizitart.no2.Nitrite
import org.dizitart.no2.NitriteCollection

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
class NitriteEntityTrashCollector(private val nitriteDb: Nitrite,
                                  private val collectionName: String = "Entity_trash") : TrashCollector {
    /**
     * Retrieve the repository on every request.
     */
    private val coll: NitriteCollection
        get() = nitriteDb.getCollection(collectionName)

    override fun view(): List<Entity> {
        val docList = coll.find().toList()

        val entityList = mutableListOf<Entity>()

        docList.forEach { entityList.add(it.toEntity()) }

        return entityList
    }

    override fun trash(entity: Entity) {
        if (coll.find("entityId" eq entity.id).firstOrNull() != null)
            throw EntityAlreadyInTrashException()

        coll.insert(entity.toDocument())
    }

    override fun restore(id: String): Entity {
        val restoredDoc = coll.find("entityId" eq id).firstOrNull()
                ?: throw EntityNotInTrashException()

        coll.remove(restoredDoc)
        return restoredDoc.toEntity()
    }

    override fun clear() {
        coll.drop()
    }
}