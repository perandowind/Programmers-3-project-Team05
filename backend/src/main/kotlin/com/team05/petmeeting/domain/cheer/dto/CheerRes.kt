package com.team05.petmeeting.domain.cheer.dto

data class CheerRes(
    val animalId: Long,
    val cheerCount: Int,  // 응원 후 총 응원수
    val temperature: Double,  // 응원 후 온도
    val remaingCheersToday: Int // 응원 후 오늘 남은 응원 횟수
)
