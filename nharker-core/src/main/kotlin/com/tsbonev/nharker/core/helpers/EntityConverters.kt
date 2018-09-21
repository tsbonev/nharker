package com.tsbonev.nharker.core.helpers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.tsbonev.nharker.core.Entity
import org.dizitart.no2.Document

/**
 * Converters used by the trash store utilizing Jackson.
 *
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */

/**
 * Converts any Entity to a document with three fields,
 * a class, an id and a json body.
 */
fun Entity.toDocument(): Document {
    val document = Document()
    document["entityId"] = this.id
    document["json"] = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .writeValueAsString(this)
    document["class"] = this::class.java.name
    return document
}

/**
 * Converts any document with a class and json field to
 * that class and the casts it as an Entity.
 */
fun Document.toEntity(): Entity {
    return ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .readValue(this["json"].toString(),
            Class.forName(this["class"].toString())) as Entity
}