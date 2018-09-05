package com.tsbonev.nharker.core

import java.time.LocalDateTime

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class Entry (val id: String,
                  val creationDate: LocalDateTime,
                  val content: String,
                  val keywords: Set<String>)
