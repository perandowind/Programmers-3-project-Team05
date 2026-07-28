package com.team05.petmeeting.domain.donation.dto

import jakarta.validation.constraints.NotBlank

data class CompleteReq(
    @field:NotBlank(message = "결제 ID를 입력해주세요.")
    val paymentId: String
)
