package com.tsbonev.nharker.core

/**
 * Provides the methods to handle a map of properties.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class ArticleProperties(private val map: MutableMap<String, Entry> = mutableMapOf()) {

    /**
     * Attaches a property to the map, replacing it if present already.
     *
     * @param propertyName The name of the property.
     * @param property The entry describing the property.
     */
    fun attachProperty(propertyName: String, property: Entry) {
        this.map[propertyName] = property
    }

    /**
     * Returns the raw map.
     *
     * @return A map of Property Names and Entries.
     */
    fun getAll(): Map<String, Entry> {
        return this.map
    }

    /**
     * Detaches a property from the map, throwing an exception
     * if it is not found.
     *
     * @param propertyName The name of the property to remove.
     * @return The removed property Entry.
     */
    fun detachProperty(propertyName: String): Entry {
        val entry = map[propertyName] ?: throw PropertyNotFoundException(propertyName)
        map.remove(propertyName)

        return entry
    }
}

class PropertyNotFoundException(val property: String) : Throwable()