package com.tsbonev.nharker.core

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class EntryRequest(val content: String,
                        val articleId: String,
                        val links: Map<String, String>)