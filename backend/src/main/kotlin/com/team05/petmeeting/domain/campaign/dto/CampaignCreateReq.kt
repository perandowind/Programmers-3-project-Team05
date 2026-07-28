package com.team05.petmeeting.domain.campaign.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class CampaignCreateReq(
    @field:NotBlank(message = "캠페인 제목을 입력해주세요.")
    val title: String,

    @field:NotBlank(message = "캠페인 설명을 입력해주세요.")
    val description: String,

    @field:Positive(message = "목표 금액은 양수여야 합니다.")
    val amount: Int
)
