package com.team05.petmeeting.domain.adoption.dto

import com.team05.petmeeting.domain.adoption.entity.AdoptionStatus
import java.time.LocalDateTime

data class AdoptionDetailRes(
    val applicationId: Long,
    val status: AdoptionStatus,
    val applyReason: String?,
    val createdAt: LocalDateTime?,
    val reviewedAt: LocalDateTime?,
    val rejectionReason: String?,
    val applyTel: String?,
    val animalInfo: AnimalInfo,
) {
    data class AnimalInfo(
        val desertionNo: String?,
        val specialMark: String?,
        val careNm: String?,
        val careOwnerNm: String?,
        val careTel: String?,
        val careAddr: String?,
    )
}
