package com.tsbonev.nharker.core

import java.time.LocalDateTime

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class Timeline(val id: String,
                    val title: String,
                    val creationDate: LocalDateTime,
                    val stories: List<Story>,
                    val articles: List<Article>)