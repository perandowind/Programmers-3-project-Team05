package com.team05.petmeeting.domain.feed.dto

import com.team05.petmeeting.domain.animal.entity.Animal

data class AdoptedAnimalRes(
    val animalId: Long?,
    val noticeNo: String?,
    val upKindNm: String?,  // 개/고양이
    val kindFullNm: String?,  // 품종
    val imageUrl: String?
) {
    companion object {
        fun from(animal: Animal): AdoptedAnimalRes {
            return AdoptedAnimalRes(
                animal.id,
                animal.noticeNo,
                animal.upKindNm,
                animal.kindFullNm,
                animal.popfile1
            )
        }
    }
}


