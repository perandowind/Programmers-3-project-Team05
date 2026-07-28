package com.team05.petmeeting.domain.adoption.dto

import com.team05.petmeeting.domain.adoption.entity.AdoptionApplication

fun AdoptionApplication.toApplyRes(): AdoptionApplyRes =
    AdoptionApplyRes(
        requireNotNull(id),
        status,
        AdoptionApplyRes.AnimalInfo(
            animal.desertionNo,
            animal.kindFullNm,
            animal.careNm,
            animal.careOwnerNm,
        ),
    )

fun AdoptionApplication.toDetailRes(): AdoptionDetailRes =
    AdoptionDetailRes(
        requireNotNull(id),
        status,
        applyReason,
        createdAt,
        reviewedAt,
        rejectionReason,
        applyTel,
        AdoptionDetailRes.AnimalInfo(
            animal.desertionNo,
            animal.specialMark,
            animal.careNm,
            animal.careOwnerNm,
            animal.careTel,
            animal.careAddr,
        ),
    )
