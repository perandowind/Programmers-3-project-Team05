package com.team05.petmeeting.domain.user.dto.profile

import com.team05.petmeeting.domain.comment.entity.AnimalComment
import java.time.LocalDateTime


data class UserAnimalCommentRes(
    val totalCommentCount: Long,
    val comments: List<AnimalCommentItem>
) {
    data class AnimalCommentItem(
        val feedId: Long,
        val desertionNo: String,
        val content: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime
    ) {
        companion object {
            @JvmStatic
            fun from(comment: AnimalComment): AnimalCommentItem {
                return AnimalCommentItem(
                    requireNotNull(comment.animal.id),
                    comment.animal.desertionNo,
                    comment.content,
                    requireNotNull(comment.createdAt),
                    requireNotNull(comment.updatedAt)
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun of(totalCommentCount: Long, commentList: List<AnimalComment>): UserAnimalCommentRes {
            val items = commentList.map(AnimalCommentItem::from)

            return UserAnimalCommentRes(totalCommentCount, items)
        }
    }
}
