package com.tsbonev.nharker.core

typealias PropertyName = String
typealias EntryReference = String

/**
 * Provides the methods to handle a map of properties.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class ArticleProperties(private val map: MutableMap<PropertyName, EntryReference> = mutableMapOf()) {

    /**
     * Attaches a property to the map, replacing it if present already.
     *
     * @param propertyName The name of the property.
     * @param entryId The id of the entry describing the property.
     */
    fun attachProperty(propertyName: String, entryId: String) {
        this.map[propertyName] = entryId
    }

    /**
     * Returns the raw map.
     *
     * @return A map of Property Names and Entry ids.
     */
    fun getAll(): Map<PropertyName, EntryReference> {
        return this.map
    }

    /**
     * Detaches a property from the map, throwing an exception
     * if it is not found.
     *
     * @param propertyName The name of the property to remove.
     * @return The removed id of the removed property entry.
     */
    fun detachProperty(propertyName: String): String {
        val entry = map[propertyName] ?: throw PropertyNotFoundException(propertyName)
        map.remove(propertyName)

        return entry
    }
}

class PropertyNotFoundException(val property: String) : Throwable()