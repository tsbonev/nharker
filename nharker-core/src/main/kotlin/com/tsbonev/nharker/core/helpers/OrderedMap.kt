package com.tsbonev.nharker.core.helpers

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */

fun <T> Map<T, Int>.switch(first: T, second: T): Map<T, Int> {
    val mutableMap = this.toMutableMap()
    val firstVal = mutableMap[first] ?: throw ElementNotInMapException()
    val secondVal = mutableMap[second] ?: throw ElementNotInMapException()

    mutableMap[first] = secondVal
    mutableMap[second] = firstVal

    return mutableMap
}

fun <T> Map<T, Int>.append(value: T): Map<T, Int> {
    val mutableMap = this.toMutableMap()
    mutableMap[value] = mutableMap.size
    return mutableMap
}

fun <T> Map<T, Int>.subtract(value: T): Map<T, Int> {
    val mutableMap = this.toMutableMap()
    val removedSpace = mutableMap[value] ?: throw ElementNotInMapException()

    mutableMap.remove(value)

    mutableMap.forEach {
        if (it.value > removedSpace) mutableMap[it.key] = it.value - 1
    }

    return mutableMap
}

class ElementNotInMapException : Throwable()
