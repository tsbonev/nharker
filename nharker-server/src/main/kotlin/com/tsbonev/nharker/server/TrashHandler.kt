package com.tsbonev.nharker.server

import java.util.Optional

/**
 * Provides the methods to handle a trash collector from
 * a higher level of abstraction.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface TrashHandler {
    /**
     * Trashes an entity.
     *
     * @param entity The entity to trash.
     * @return The trashed entity's id.
     */
    fun trash(entity: Any): String

    /**
     * Restores an entity.
     *
     * @param trashedEntityId The id of the entity to be restored.
     * @param entityClass The class of the entity.
     * @return An optional restored entity of the given class.
     */
    fun <T> restore(trashedEntityId: String, entityClass: Class<T>): Optional<T>

    /**
     * Returns a list of entities that match a requested class.
     *
     * @param entityClass The class to look for.
     * @return A list of entities matching the requested class.
     */
    fun <T> view(entityClass: Class<T>): List<T>
}