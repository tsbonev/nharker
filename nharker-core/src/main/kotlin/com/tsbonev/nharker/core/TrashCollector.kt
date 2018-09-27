package com.tsbonev.nharker.core

/**
 * Provides the methods to store a deleted entity in a
 * collection and be retrieved on a later basis.
 *
 * An Entity is any domain object.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface TrashCollector {
    /**
     * Returns a list of trashed entities.
     *
     * @return The list of trashed entities.
     */
    fun view(): List<Any>

    /**
     * Stores an entity into the trash collection.
     *
     * @param entity Entity to trash.
     * @return The id of the trashed entity.
     */
    fun trash(entity: Any): String

    /**
     * Retrieves an entity from the trash.
     *
     * @param id The id of the entity.
     * @return The restored entity.
     */
    @Throws(EntityNotInTrashException::class)
    fun restore(id: String): Any

    /**
     * Clears all trashed entities.
     */
    fun clear()
}

class EntityNotInTrashException : Throwable()