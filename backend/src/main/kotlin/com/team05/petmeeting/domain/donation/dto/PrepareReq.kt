package com.team05.petmeeting.domain.donation.dto

import jakarta.validation.constraints.Positive

data class PrepareReq(
    @field:Positive(message = "캠페인 ID는 양수여야 합니다.")
    val campaignId: Long,

    @field:Positive(message = "후원 금액은 양수여야 합니다.")
    val amount: Int
)
