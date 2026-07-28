package com.team05.petmeeting.domain.comment.repository

import com.team05.petmeeting.domain.comment.entity.FeedComment
import com.team05.petmeeting.domain.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface FeedCommentRepository : JpaRepository<FeedComment, Long> {
    fun findByFeed_Id(feedId: Long): List<FeedComment>

    fun findByFeed_Id(feedId: Long, pageable: Pageable): Page<FeedComment>

    fun findAllByUserOrderByCreatedAtDesc(user: User): List<FeedComment>

    fun countFeedCommentByUser(user: User): Long
}