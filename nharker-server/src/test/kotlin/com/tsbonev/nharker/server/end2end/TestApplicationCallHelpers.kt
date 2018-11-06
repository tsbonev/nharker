package com.tsbonev.nharker.server.end2end

import com.google.gson.Gson
import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.core.Entry
import io.ktor.server.testing.TestApplicationCall

/**
 * Helpers that use Gson for cleaner usage of the TestApplicationCall object.
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */

/**
 * Converts any object to json using Gson.
 */
fun Any.toJson() : String {
	return Gson().toJson(this)
}

/**
 * Converts any string json to an object using Gson.
 */
fun <T> String.fromJson(clazz: Class<T>) : T {
	return Gson().fromJson(this, clazz)
}

/**
 * Converts the content of a TestApplicationResponse to an object.
 */
fun <T> TestApplicationCall.contentAsObject(clazz: Class<T>) : T {
	return this.response.content!!.fromJson(clazz)
}

/**
 * Converts the content of a TestApplicationResponse to a catalogue.
 */
fun TestApplicationCall.asCatalogue() : Catalogue {
	return this.contentAsObject(Catalogue::class.java)
}

/**
 * Converts the content of a TestApplicationResponse to an article.
 */
fun TestApplicationCall.asArticle() : Article {
	return this.contentAsObject(Article::class.java)
}

/**
 * Converts the content of a TestApplicationResponse to an entry.
 */
fun TestApplicationCall.asEntry() : Entry {
	return this.contentAsObject(Entry::class.java)
}
