package com.team05.petmeeting.domain.feed.repository

import com.team05.petmeeting.domain.feed.entity.Feed
import com.team05.petmeeting.domain.feed.entity.FeedLike
import com.team05.petmeeting.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface FeedLikeRepository : JpaRepository<FeedLike, Long> {

    // 좋아요 추가(중복 체크)
    fun existsByUserAndFeed(user: User, feed: Feed): Boolean

    // 좋아요 취소
    fun findByUserAndFeed(user: User, feed: Feed): Optional<FeedLike>

    fun countByFeed(feed: Feed): Long
}
