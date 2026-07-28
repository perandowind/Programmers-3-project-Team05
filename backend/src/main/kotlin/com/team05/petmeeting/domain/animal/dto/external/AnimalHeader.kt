package com.team05.petmeeting.domain.animal.dto.external

// 외부 API 처리 결과 코드와 메시지를 담는 header
data class AnimalHeader @JvmOverloads constructor(
    var reqNo: Long? = null,
    var resultCode: String? = null,
    var resultMsg: String? = null,
)
