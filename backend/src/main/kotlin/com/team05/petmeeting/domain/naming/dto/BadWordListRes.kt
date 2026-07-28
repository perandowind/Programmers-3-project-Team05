package com.team05.petmeeting.domain.naming.dto

// 금칙어 전체 조회 응답 DTO
data class BadWordListRes(
    val badWords: List<BadWordDto>,
    val totalCount: Int
) {
    data class BadWordDto(
        val wordId: Long,
        val word: String,
        val addedAt: String
    )
}