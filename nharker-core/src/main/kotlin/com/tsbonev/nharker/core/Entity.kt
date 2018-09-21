package com.tsbonev.nharker.core

/**
 * Entity is the base of all domain object, it enforces all of them
 * to have an id and allows for less duplication when working with
 * generic collections.
 *
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
open class Entity(open val id: String)