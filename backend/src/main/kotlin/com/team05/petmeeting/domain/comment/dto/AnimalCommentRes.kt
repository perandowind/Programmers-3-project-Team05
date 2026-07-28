package com.team05.petmeeting.domain.comment.dto

import com.team05.petmeeting.domain.comment.entity.AnimalComment
import java.time.LocalDateTime

data class AnimalCommentRes(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val commentId: Long,
    val content: String,
    val feedId: Long,
    val createdAt: LocalDateTime
) {
    companion object {
        @JvmStatic
        fun from(comment: AnimalComment): AnimalCommentRes {
            return AnimalCommentRes(
                requireNotNull(comment.user.id),
                comment.user.nickname,
                comment.user.profileImageUrl,
                requireNotNull(comment.id),
                comment.content,
                requireNotNull(comment.animal.id),
                requireNotNull(comment.createdAt)
            )
        }
    }
}
