package com.team05.petmeeting.domain.comment.dto

import org.springframework.data.domain.Page

data class AnimalCommentListRes(
    val comments: List<AnimalCommentRes>,
    val totalCount: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int
) {
    companion object {
        @JvmStatic
        fun from(comments: List<AnimalCommentRes>): AnimalCommentListRes {
            return AnimalCommentListRes(
                comments = comments,
                totalCount = comments.size.toLong(),
                page = 0,
                size = comments.size,
                totalPages = 1
            )
        }

        @JvmStatic
        fun from(page: Page<AnimalCommentRes>): AnimalCommentListRes {
            return AnimalCommentListRes(
                comments = page.content,
                totalCount = page.totalElements,
                page = page.number,
                size = page.size,
                totalPages = page.totalPages
            )
        }
    }
}

