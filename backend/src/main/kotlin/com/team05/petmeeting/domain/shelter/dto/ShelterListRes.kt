package com.team05.petmeeting.domain.shelter.dto

data class ShelterListRes(
    val shelters: List<ShelterRes>,
    val totalCount: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int
)