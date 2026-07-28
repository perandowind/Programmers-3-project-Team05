package com.team05.petmeeting.domain.feed.dto

import com.team05.petmeeting.domain.comment.dto.FeedCommentRes
import com.team05.petmeeting.domain.feed.entity.Feed
import com.team05.petmeeting.domain.feed.enums.FeedCategory
import java.time.LocalDateTime

data class FeedRes(
    val profileImageUrl: String?,
    val nickname: String,
    val feedId: Long,
    val userId: Long?,
    val animalId: Long?,
    val category: FeedCategory,
    val title: String,
    val content: String,
    val imageUrl: String?,
    val likeCount: Int,
    val commentCount: Int,
    val comments: List<FeedCommentRes>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(feed: Feed, likeCount: Int): FeedRes {
            val user = feed.user

            return FeedRes(
                profileImageUrl = user.profileImageUrl,
                nickname = user.nickname,
                feedId = requireNotNull(feed.id),
                userId = user.id,
                animalId = feed.animal?.id,
                category = feed.category,
                title = feed.title,
                content = feed.content,
                imageUrl = feed.imageUrl,
                likeCount = likeCount,
                commentCount = feed.comments.size,
                comments = feed.comments.map { FeedCommentRes.from(it) },
                createdAt = feed.createdAt,
                updatedAt = feed.updatedAt
            )
        }
    }
}
