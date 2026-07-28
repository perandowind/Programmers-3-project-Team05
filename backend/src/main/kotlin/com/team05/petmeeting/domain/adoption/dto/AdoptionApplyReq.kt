package com.team05.petmeeting.domain.adoption.dto

import jakarta.validation.constraints.NotBlank

data class AdoptionApplyReq(
    @field:NotBlank(message = "입양 신청 사유를 입력해주세요.")
    val applyReason: String,

    @field:NotBlank(message = "연락처를 입력해주세요.")
    val applyTel: String,
)
