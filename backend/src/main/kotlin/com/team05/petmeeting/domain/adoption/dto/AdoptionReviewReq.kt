package com.team05.petmeeting.domain.adoption.dto

import com.team05.petmeeting.domain.adoption.entity.AdoptionStatus
import jakarta.validation.constraints.NotNull

data class AdoptionReviewReq(
    @field:NotNull(message = "검토 상태를 입력해주세요.")
    val status: AdoptionStatus,
    val rejectionReason: String? = null,
)
