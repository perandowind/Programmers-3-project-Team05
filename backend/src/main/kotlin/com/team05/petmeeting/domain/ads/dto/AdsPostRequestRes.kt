package com.team05.petmeeting.domain.ads.dto

import com.team05.petmeeting.domain.ads.entity.AdsPostRequest
import com.team05.petmeeting.domain.ads.entity.AdsPostStatus
import java.time.LocalDateTime

data class AdsPostRequestRes(
    val requestId: Long,
    val status: AdsPostStatus,
    val imageUrl: String,
    val caption: String,
    val createdAt: LocalDateTime?,
    val reviewedAt: LocalDateTime?,
    val publishedAt: LocalDateTime?,
    val rejectionReason: String?,
    val animalInfo: AnimalInfo,
) {
    data class AnimalInfo(
        val desertionNo: String,
        val kindFullNm: String,
        val age: String?,
        val sexCd: String?,
        val careRegNo: String,
        val careNm: String?,
        val specialMark: String?,
    )

    companion object {
        fun from(request: AdsPostRequest): AdsPostRequestRes =
            AdsPostRequestRes(
                requestId = requireNotNull(request.id),
                status = request.status,
                imageUrl = request.imageUrl,
                caption = request.caption,
                createdAt = request.createdAt,
                reviewedAt = request.reviewedAt,
                publishedAt = request.publishedAt,
                rejectionReason = request.rejectionReason,
                animalInfo = AnimalInfo(
                    desertionNo = request.animal.desertionNo,
                    kindFullNm = request.animal.kindFullNm,
                    age = request.animal.age,
                    sexCd = request.animal.sexCd,
                    careRegNo = request.shelter.careRegNo,
                    careNm = request.shelter.careNm,
                    specialMark = request.animal.specialMark,
                ),
            )
    }
}
