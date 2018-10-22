package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.PaginationException
import com.tsbonev.nharker.core.SortBy
import org.dizitart.kno2.nitrite
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitritePaginatorTest {

    private val db = nitrite { }

    private val paginator = NitritePaginator<Entry>(db.getRepository(Entry::class.java))

    private val firstEntry = Entry(
            "::first-entry-id::",
            LocalDateTime.ofEpochSecond(1, 1, ZoneOffset.UTC),
            "::article-id::",
            "::content::"
    )
    private val secondEntry = Entry(
            "::second-entry-id::",
            LocalDateTime.ofEpochSecond(2, 2, ZoneOffset.UTC),
            "::article-id::",
            "::content::"
    )
    private val thirdEntry = Entry(
            "::third-entry-id::",
            LocalDateTime.ofEpochSecond(3, 3, ZoneOffset.UTC),
            "::article-id::",
            "::content::"
    )

    @Before
    fun setUp() {
        db.getRepository(Entry::class.java).insert(arrayOf(firstEntry, secondEntry, thirdEntry))
    }

    @Test
    fun `Get all objects sorted`() {
        val paginatedDescendingList = paginator.getAll(SortBy.DESCENDING)
        val paginatedAscendingList = paginator.getAll(SortBy.ASCENDING)

        assertThat(paginatedDescendingList, Is(listOf(thirdEntry, secondEntry, firstEntry)))
        assertThat(paginatedAscendingList, Is(listOf(firstEntry, secondEntry, thirdEntry)))
    }

    @Test
    fun `Get paginated objects`() {
        val paginatedDescendingList = paginator.getAll(SortBy.DESCENDING, 2, 2)
        val paginatedAscendingList = paginator.getAll(SortBy.ASCENDING, 1, 3)

        assertThat(paginatedDescendingList, Is(listOf(firstEntry)))
        assertThat(paginatedAscendingList, Is(listOf(firstEntry, secondEntry, thirdEntry)))
    }

    @Test
    fun `Larger page sizes get filled`() {
        assertThat(paginator.getAll(SortBy.ASCENDING, 1, 100),
                Is(listOf(firstEntry, secondEntry, thirdEntry)))
    }

    @Test
    fun `Overextended pages return empty`() {
        assertThat(paginator.getAll(SortBy.ASCENDING, 2, 100),
                Is(emptyList()))
    }

    @Test(expected = PaginationException::class)
    fun `Paginating with size less than zero exception`() {
        paginator.getAll(SortBy.ASCENDING, 1, -1)
    }

    @Test(expected = PaginationException::class)
    fun `Paginating with page less than one exception`() {
        paginator.getAll(SortBy.ASCENDING, 0, 2)
    }
}