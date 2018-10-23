package com.tsbonev.nharker.core

import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class OrderedReferenceMapTest {

    private val map = OrderedReferenceMap()

    @Test
    fun `Appending to map follows order`() {
        map.append("One")
        map.append("Two")

        assertThat(map.contains("One"), Is(true))
        assertThat(map.contains("Two"), Is(true))
        assertThat(map.contains("Three"), Is(false))
    }

    @Test
    fun `Subtracting from map reorders`() {
        map.append("One")
        map.append("Two")
        map.append("Three")

        map.subtract("Two")

        assertThat(map.raw()["One"]!!, Is(0))
        assertThat(map.raw()["Three"]!!, Is(1))
    }

    @Test(expected = ElementNotInMapException::class)
    fun `Subtracting non-existent id throws exception`() {
        map.subtract("::non-existent-id::")
    }

    @Test
    fun `Switching list ids reorders`() {
        map.append("One")
        map.append("Two")
        map.append("Three")

        map.switch("One", "Three")

        assertThat(map.raw()["One"]!!, Is(2))
        assertThat(map.raw()["Three"]!!, Is(0))
        assertThat(map.raw()["Two"]!!, Is(1))
    }

    @Test(expected = ElementNotInMapException::class)
    fun `Switching non-existent id throws exception`() {
        map.switch("::non-existent-id::", "::non-existent-id::")
    }
}