package com.team05.petmeeting.domain.animal.dto.external

// 외부 API의 header/body 래퍼 객체
data class AnimalResponse @JvmOverloads constructor(
    var header: AnimalHeader? = null,
    var body: AnimalBody? = null,
)
