package com.tsbonev.nharker.core

import java.time.LocalDateTime

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class Article(val id: String,
                   val title: String,
                   val creationDate: LocalDateTime,
                   val properties: Map<String, Entry>,
                   val entries: List<Entry>,
                   val keywords: Set<String>)