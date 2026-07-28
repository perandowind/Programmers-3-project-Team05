package com.team05.petmeeting.domain.feed.dto

import com.team05.petmeeting.domain.feed.entity.Feed
import com.team05.petmeeting.domain.feed.enums.FeedCategory
import java.time.LocalDateTime

data class FeedListRes(
    val feedId: Long,
    val userId: Long,
    val profileImageUrl: String?,
    val animalId: Long?,
    val nickname: String,
    val category: FeedCategory,
    val title: String,
    val content: String,
    val imageUrl: String?,
    val likeCount: Int,
    val isLiked: Boolean,
    val commentCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(
            feed: Feed,
            userId: Long,
            profileImageUrl: String?,
            nickname: String,
            animalId: Long?,
            likeCount: Long,
            commentCount: Long,
            isLiked: Boolean
        ) = FeedListRes(
            requireNotNull(feed.id),
            userId,
            profileImageUrl,
            animalId,
            nickname,
            feed.category,
            feed.title,
            feed.content,
            feed.imageUrl,
            likeCount.toInt(),
            isLiked,
            commentCount.toInt(),
            requireNotNull(feed.createdAt),
            feed.updatedAt
        )
    }
}
