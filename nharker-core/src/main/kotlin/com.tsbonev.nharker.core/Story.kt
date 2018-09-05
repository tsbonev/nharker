package com.tsbonev.nharker.core

import java.time.LocalDateTime

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class Story (val id: String,
                  val title: String,
                  val creationDate: LocalDateTime,
                  val chapters: List<Chapter>,
                  val articles: List<Article>,
                  val keywords: Set<String>)