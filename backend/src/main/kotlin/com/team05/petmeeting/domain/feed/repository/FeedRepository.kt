package com.team05.petmeeting.domain.feed.repository

import com.team05.petmeeting.domain.feed.entity.Feed
import com.team05.petmeeting.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface FeedRepository : JpaRepository<Feed, Long>, FeedRepositoryCustom {

    fun countByUser(user: User): Long

    fun findAllByUserOrderByCreatedAtDesc(user: User): MutableList<Feed>
}