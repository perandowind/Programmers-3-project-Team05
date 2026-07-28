package com.team05.petmeeting.domain.shelter.dto

import java.time.LocalDateTime

data class ShelterCommand(
    val careRegNo: String,
    val careNm: String?,
    val careTel: String?,
    val careAddr: String?,
    val careOwnerNm: String?,
    val orgNm: String?,
    val updTm: LocalDateTime
)
