package com.team05.petmeeting.domain.adoption.dto

import com.team05.petmeeting.domain.adoption.entity.AdoptionStatus

data class AdoptionApplyRes(
    val applicationId: Long,
    val status: AdoptionStatus,
    val animalInfo: AnimalInfo,
) {
    data class AnimalInfo(
        val desertionNo: String?,
        val kindFullNm: String?,
        val careNm: String?,
        val careOwnerNm: String?,
    )
}
