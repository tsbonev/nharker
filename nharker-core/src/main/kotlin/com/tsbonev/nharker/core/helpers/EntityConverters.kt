package com.tsbonev.nharker.core.helpers

import com.google.gson.Gson
import com.tsbonev.nharker.core.Entity
import org.dizitart.no2.Document

/**
 * Converters used by the trash store.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */

/**
 * Converts an entity to a document with three fields:
 * a class, an id and a json body.
 *
 * @return The converted document.
 */
fun Entity.toDocument(): Document {
    val document = Document()
    document["entityId"] = this.id
    document["json"] = Gson().toJson(this)
    document["class"] = this::class.java.name
    return document
}

/**
 * Converts any document with a class and json field to
 * that class.
 *
 * @return The converted entity.
 */
fun Document.toEntity(): Any {
    return Gson().fromJson(this["json"].toString(),
            Class.forName(this["class"].toString()))
}