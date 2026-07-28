package com.team05.petmeeting.domain.ads.dto

import com.team05.petmeeting.domain.ads.entity.AdsPostStatus
import jakarta.validation.constraints.NotNull

data class AdsPostReviewReq(
    @field:NotNull(message = "광고 게시 심사 상태를 입력해주세요.")
    val status: AdsPostStatus,
    val rejectionReason: String? = null,
)
