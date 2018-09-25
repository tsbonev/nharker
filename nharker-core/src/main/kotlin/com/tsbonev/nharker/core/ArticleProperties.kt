package com.tsbonev.nharker.core

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class ArticleProperties (private val map: MutableMap<String, Entry> = mutableMapOf()){
    fun attachProperty(propertyName: String, property: Entry){
        this.map[propertyName] = property
    }

    fun getAll(): Map<String, Entry>{
        return this.map
    }

    fun detachProperty(propertyName: String): Entry{
        val entry = map[propertyName] ?: throw PropertyNotFoundException()
        map.remove(propertyName)

        return entry
    }
}

class PropertyNotFoundException : Exception()