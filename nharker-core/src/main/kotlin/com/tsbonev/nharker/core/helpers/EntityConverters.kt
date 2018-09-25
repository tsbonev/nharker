package com.tsbonev.nharker.core.helpers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.dizitart.no2.Document
import org.dizitart.no2.NitriteId

/**
 * Converters used by the trash store utilizing Jackson.
 *
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */

/**
 * Converts object to a document with three fields,
 * a class, an id and a json body.
 */
fun Any.toDocument(): Document {
    val document = Document()
    document["entityId"] = NitriteId.newId().toString()
    document["json"] = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .writeValueAsString(this)
    document["class"] = this::class.java.name
    return document
}

/**
 * Converts any document with a class and json field to
 * that class.
 */
fun Document.toEntity(): Any {
    return ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .readValue(this["json"].toString(),
            Class.forName(this["class"].toString()))
}