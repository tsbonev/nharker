package com.tsbonev.nharker.core

/**
 * A request object containing all of the information necessary
 * for the creation of an Entry object.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class EntryRequest(
	val content: String,
	val articleId: String,
	val links: Map<String, String>
)