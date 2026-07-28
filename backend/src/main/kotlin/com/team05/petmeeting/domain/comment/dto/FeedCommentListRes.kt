package com.team05.petmeeting.domain.comment.dto

import org.springframework.data.domain.Page

data class FeedCommentListRes(
    val comments: List<FeedCommentRes>,
    val totalCount: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int
) {
    companion object {
        @JvmStatic
        fun from(comments: List<FeedCommentRes>): FeedCommentListRes {
            return FeedCommentListRes(
                comments = comments,
                totalCount = comments.size.toLong(),
                page = 0,
                size = comments.size,
                totalPages = 1
            )
        }

        @JvmStatic
        fun from(page: Page<FeedCommentRes>): FeedCommentListRes {
            return FeedCommentListRes(
                comments = page.content,
                totalCount = page.totalElements,
                page = page.number,
                size = page.size,
                totalPages = page.totalPages
            )
        }
    }
}
