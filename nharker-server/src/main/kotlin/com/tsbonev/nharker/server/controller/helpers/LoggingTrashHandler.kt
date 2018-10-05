package com.tsbonev.nharker.server.controller.helpers

import com.tsbonev.nharker.core.EntityNotInTrashException
import com.tsbonev.nharker.core.TrashCollector
import org.slf4j.LoggerFactory
import java.lang.ClassCastException
import java.util.Optional

/**
 * Provides the methods to trash and restore entities while
 * logging its actions and handling exceptions.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class LoggingTrashHandler(private val trashCollector: TrashCollector) : TrashHandler {

    private val logger = LoggerFactory.getLogger("LoggingTrashHandler")

    override fun trash(entity: Any): String {
        val trashedId = trashCollector.trash(entity)
        logger.info("Trashing entity with id: $trashedId")

        return trashedId
    }

    /**
     * If the entity is not found, logs the id requested and returns empty.
     * If the entity cannot be cast, logs the id requested and returns empty.
     */
    override fun <T> restore(trashedEntityId: String, entityClass: Class<T>): Optional<T> {
        return try {
            val restoredEntity = trashCollector.restore(trashedEntityId)
            logger.info("Restoring entity with id: $trashedEntityId")

            Optional.of(entityClass.cast(restoredEntity))
        } catch (ex: EntityNotInTrashException) {
            logger.error("Requested entity with id: $trashedEntityId was not found in the trash.")
            Optional.empty()
        } catch (ex: ClassCastException) {
            logger.error("Requested entity with id: $trashedEntityId was restored, " +
                    "but could not be cast to ${entityClass.name}")
            Optional.empty()
        }
    }

    override fun <T> view(entityClass: Class<T>): List<T> {
        val entityList = mutableListOf<T>()

        trashCollector
                .view()
                .asSequence()
                .filter {
                    entityClass.isInstance(it)
                }
                .mapTo(entityList) {
                    entityClass.cast(it)
                }

        logger.info("Returned a list of ${entityClass.name} with size ${entityList.size}")

        return entityList
    }
}