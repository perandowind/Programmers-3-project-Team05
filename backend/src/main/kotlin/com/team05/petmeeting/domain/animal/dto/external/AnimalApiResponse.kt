package com.team05.petmeeting.domain.animal.dto.external

// 외부 유기동물 API 최상위 응답 객체
data class AnimalApiResponse @JvmOverloads constructor(
    var response: AnimalResponse? = null,
)
