package com.team05.petmeeting.domain.naming.dto

// 투표 DTO
data class NameVoteRes(
    val candidateId: Long,
    val currentVoteCount: Int
)
