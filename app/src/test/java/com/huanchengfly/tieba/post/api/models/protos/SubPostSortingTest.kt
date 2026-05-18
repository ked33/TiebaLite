package com.huanchengfly.tieba.post.api.models.protos

import org.junit.Assert.assertEquals
import org.junit.Test

class SubPostSortingTest {
    @Test
    fun sortedByReplyTimeAsc_ordersSubPostsByOldestReplyFirst() {
        val highAgreeReply = SubPostList(id = 3L, time = 300, floor = 3)
        val firstReply = SubPostList(id = 1L, time = 100, floor = 1)
        val secondReply = SubPostList(id = 2L, time = 200, floor = 2)

        val sortedIds = listOf(highAgreeReply, firstReply, secondReply)
            .sortedByReplyTimeAsc()
            .map { it.id }

        assertEquals(listOf(1L, 2L, 3L), sortedIds)
    }
}
