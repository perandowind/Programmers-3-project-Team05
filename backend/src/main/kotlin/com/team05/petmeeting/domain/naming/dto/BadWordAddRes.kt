package com.team05.petmeeting.domain.naming.dto

import java.time.LocalDateTime

// 금칙어 추가 성공 응답 DTO
data class BadWordAddRes(
    val wordId: Long,
    val badWord: String,
    val addedAt: LocalDateTime
)