package com.tsbonev.nharker.core

/**
 * A wrapper of a linked hash map that provides the functionality
 * to switch the order of elements and enforce their general order
 * based on their values rather than keys.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class OrderedReferenceMap(private val map: LinkedHashMap<String, Int> = linkedMapOf()) {
    /**
     * Switches the order of two elements in the ordered map.
     *
     * @param firstRef The first element to switch.
     * @param secondRef The second element to switch.
     *
     * If one of the elements is not in the map, throws exception.
     */
    fun switch(firstRef: String, secondRef: String) {
        val firstOrder = map[firstRef] ?: throw ElementNotInMapException(firstRef)
        val secondOrder = map[secondRef] ?: throw ElementNotInMapException(secondRef)

        map[firstRef] = secondOrder
        map[secondRef] = firstOrder

        this.sortByValues()
    }

    /**
     * Appends an element to the ordered map.
     *
     * @param reference The element to append.
     */
    fun append(reference: String) {
        map[reference] = map.size
    }

    /**
     * Subtracts an element from an ordered map and rearranges the rest
     * of the elements.
     *
     * @param reference The value to subtract.
     */
    fun subtract(reference: String) {
        val removedSpace = map[reference] ?: throw ElementNotInMapException(reference)

        map.remove(reference)

        map.forEach {
            if (it.value > removedSpace) map[it.key] = it.value - 1
        }
    }

    /**
     * Returns the raw map.
     *
     * @return The ordered map.
     */
    fun raw(): Map<String, Int> {
        return map
    }

    /**
     * Checks if a reference is contained in the ordered map.
     *
     * @param reference The reference to check for.
     * @return Whether the reference is in the map or not.
     */
    fun contains(reference: String): Boolean {
        return map.containsKey(reference)
    }

    /**
     * Sorts a map by its int values.
     */
    private fun sortByValues() {
        map.toList()
                .sortedBy { (_, order) ->
                    order
                }
                .toMap(map)
    }
}

class ElementNotInMapException(val reference: String) : Throwable()