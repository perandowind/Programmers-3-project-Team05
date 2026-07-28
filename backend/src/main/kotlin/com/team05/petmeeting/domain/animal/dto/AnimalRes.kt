package com.team05.petmeeting.domain.animal.dto

import com.team05.petmeeting.domain.animal.entity.Animal

data class AnimalRes(
    val animalId: Long,
    val desertionNo: String,
    val processState: String?,
    val noticeNo: String?,
    val noticeEdt: String,

    val upKindNm: String,
    val kindFullNm: String,
    val colorCd: String?,
    val age: String?,
    val weight: String?,
    val sexCd: String?,

    val popfile1: String,
    val popfile2: String?,

    val careNm: String?,
    val careTel: String?,
    val careAddr: String?,

    val totalCheerCount: Int,
    val temperature: Double,

    val care_reg_no: String?,
    val specialMark: String?,

    val recommendationReason: String? = null
) {
    constructor(animal: Animal) : this(
        requireNotNull(animal.id),
        animal.desertionNo,
        animal.processState,
        animal.noticeNo,
        animal.noticeEdt.toString(),

        animal.upKindNm,
        animal.kindFullNm,
        animal.colorCd,
        animal.age,
        animal.weight,
        animal.sexCd,

        animal.popfile1,
        animal.popfile2,

        animal.careNm,
        animal.careTel,
        animal.careAddr,

        animal.totalCheerCount,
        animal.getTemperature(),

        animal.shelter?.careRegNo,
        animal.specialMark
    )
}
