package com.tsbonev.nharker.core.helpers

import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class OrderedMapHelpersTest {

    private lateinit var holderMap : Map<String, Int>
    private val mutableMap = mutableMapOf<String, Int>()

    @Test
    fun `Appending to map follows order`(){
        holderMap = mutableMap.append("One")
        holderMap = holderMap.append("Two")

        assertThat(holderMap.contains("One"), Is(true))
        assertThat(holderMap.contains("Two"), Is(true))
        assertThat(holderMap.contains("Three"), Is(false))
    }

    @Test
    fun `Subtracting from map reorders`(){
        holderMap = mutableMap.append("One")
        holderMap = holderMap.append("Two")
        holderMap = holderMap.append("Three")

        holderMap = holderMap.subtract("Two")

        assertThat(holderMap["One"]!!, Is(0))
        assertThat(holderMap["Three"]!!, Is(1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Subtracting non-existent value throws exception`(){
        mutableMap.subtract("::non-existent-value::")
    }

    @Test
    fun `Switching list values reorders`(){
        holderMap = mutableMap.append("One")
        holderMap = holderMap.append("Two")
        holderMap = holderMap.append("Three")

        holderMap = holderMap.switch("One", "Three")

        assertThat(holderMap["One"]!!, Is(2))
        assertThat(holderMap["Three"]!!, Is(0))
        assertThat(holderMap["Two"]!!, Is(1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Switching non-existent value throws exception`(){
        mutableMap.switch("::non-existent-value::", "::non-existent-value::")
    }
}