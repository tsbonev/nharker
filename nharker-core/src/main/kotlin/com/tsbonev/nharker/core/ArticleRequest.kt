package com.tsbonev.nharker.core

/**
 * A request object containing all of the necessary
 * information for the creation of an Article object.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class ArticleRequest(
	val fullTitle: String,
	val catalogues: Set<String>
)