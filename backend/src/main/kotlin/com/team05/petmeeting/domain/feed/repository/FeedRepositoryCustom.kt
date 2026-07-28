package com.team05.petmeeting.domain.feed.repository

import com.team05.petmeeting.domain.feed.dto.FeedListRes
import com.team05.petmeeting.domain.feed.enums.FeedCategory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface FeedRepositoryCustom {
    fun findFeeds(
        pageable: Pageable,
        userId: Long?,
        category: FeedCategory?
    ): Page<FeedListRes>
}