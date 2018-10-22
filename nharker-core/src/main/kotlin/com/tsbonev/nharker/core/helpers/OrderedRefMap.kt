package com.tsbonev.nharker.core.helpers

/**
 * Helper extension functions that handle a map of format Map<Any, Int>.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */

/**
 * Aliases to better convey what the parts of an ordered ref map are.
 */
typealias Order = Int
typealias Reference = String

/**
 * An alias used to identify an ordered reference map.
 */
typealias OrderedRefMap = Map<Reference, Order>

/**
 * Switches the order of two elements in a map.
 *
 * @param first The first element to switch.
 * @param second The second element to switch.
 * @return A new map with the elements switched.
 *
 * If one of the elements is not in the map, throws exception.
 */
fun <T : Any> Map<T, Order>.switch(first: T, second: T): Map<T, Order> {
    val mutableMap = this.toMutableMap()
    val firstVal = mutableMap[first] ?: throw ElementNotInMapException(first)
    val secondVal = mutableMap[second] ?: throw ElementNotInMapException(second)

    mutableMap[first] = secondVal
    mutableMap[second] = firstVal

    return mutableMap
}

/**
 * Appends an element to an ordered map.
 *
 * @param value The element to append.
 * @return A new map with the appended element.
 */
fun <T> Map<T, Order>.append(value: T): Map<T, Order> {
    val mutableMap = this.toMutableMap()
    mutableMap[value] = mutableMap.size
    return mutableMap
}

/**
 * Subtracts an element from an ordered map and rearranges the rest
 * of the elements.
 *
 * @param value The value to subtract.
 * @return A new map with the value subtracted.
 */
fun <T : Any> Map<T, Order>.subtract(value: T): Map<T, Order> {
    val mutableMap = this.toMutableMap()
    val removedSpace = mutableMap[value] ?: throw ElementNotInMapException(value)

    mutableMap.remove(value)

    mutableMap.forEach {
        if (it.value > removedSpace) mutableMap[it.key] = it.value - 1
    }

    return mutableMap
}

/**
 * Sorts a map by its int values.
 *
 * @return A new map with sorted by value pairs.
 */
fun <T> Map<T, Order>.sortByValues(): Map<T, Order> {
    return this.toList()
            .sortedBy {
                (_, order) -> order
            }
            .toMap()
}

class ElementNotInMapException(val element: Any) : Throwable()
