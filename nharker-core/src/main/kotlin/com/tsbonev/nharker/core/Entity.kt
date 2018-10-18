package com.tsbonev.nharker.core

import java.time.LocalDateTime

/**
 * Defines what a Domain Entity must contain to
 * be considered a Domain Entity.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface Entity {
    val id: String
    val creationDate: LocalDateTime
}
