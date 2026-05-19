package com.huanchengfly.tieba.post.api

import org.junit.Assert.assertEquals
import org.junit.Test

class ForumSortTypeTest {
    @Test
    fun valueOf_returnsHotSortType() {
        assertEquals(ForumSortType.HOT, ForumSortType.valueOf(3))
    }
}
