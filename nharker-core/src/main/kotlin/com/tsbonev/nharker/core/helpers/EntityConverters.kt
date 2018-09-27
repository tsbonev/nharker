package com.tsbonev.nharker.core.helpers

import com.google.gson.Gson
import org.dizitart.no2.Document
import org.dizitart.no2.NitriteId

/**
 * Converters used by the trash store.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */

/**
 * Converts object to a document with three fields:
 * a class, an id and a json body.
 */
fun Any.toDocument(): Document {
    val document = Document()
    document["entityId"] = NitriteId.newId().toString()
    document["json"] = Gson().toJson(this)
    document["class"] = this::class.java.name
    return document
}

/**
 * Converts any document with a class and json field to
 * that class.
 */
fun Document.toEntity(): Any {
    return Gson().fromJson(this["json"].toString(),
            Class.forName(this["class"].toString()))
}