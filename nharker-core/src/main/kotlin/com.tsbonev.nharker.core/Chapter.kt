package com.tsbonev.nharker.core

import java.time.LocalDateTime

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class Chapter (val id: String,
                    val title: String,
                    val creationDate: LocalDateTime,
                    val articles: List<Article>,
                    val keywords: Set<String>)