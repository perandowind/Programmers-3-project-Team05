package com.team05.petmeeting.domain.animal.dto.external

// 외부 API 동물 목록 배열 래퍼
data class AnimalItems @JvmOverloads constructor(
    var item: List<AnimalItem>? = null,
)
