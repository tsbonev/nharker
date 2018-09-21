package com.tsbonev.nharker.core

import java.lang.Exception

/**
 * Provides the methods to store a deleted entity in a
 * collection and be retrieved on a later basis.
 *
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
interface TrashCollector {
    /**
     * Returns a list of trashed entities.
     *
     * @return The list of trashed entities.
     */
    fun view(): List<Entity>

    /**
     * Stores an entity into the trash collection.
     *
     * @param entity Entity to trash.
     */
    @Throws(EntityAlreadyInTrashException::class)
    fun trash(entity: Entity)

    /**
     * Retrieves an entity from the trash.
     *
     * @param id The id of the entity.
     * @return The restored entity.
     */
    @Throws(EntityNotInTrashException::class)
    fun restore(id: String): Entity

    /**
     * Clears all trashed entities.
     */
    fun clear()
}

class EntityAlreadyInTrashException: Exception()
class EntityNotInTrashException: Exception()