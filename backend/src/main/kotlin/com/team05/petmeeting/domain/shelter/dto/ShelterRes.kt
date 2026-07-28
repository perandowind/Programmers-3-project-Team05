package com.team05.petmeeting.domain.shelter.dto

import com.team05.petmeeting.domain.shelter.entity.Shelter

data class ShelterRes(
    val shelterId: String,  // careRegNo
    val careNm: String?,
    val careTel: String?,
    val careAddr: String?,
    val orgNm: String?
) {
    companion object {
        @JvmStatic
        fun from(shelter: Shelter): ShelterRes {
            return ShelterRes(
                shelter.careRegNo,
                shelter.careNm,
                shelter.careTel,
                shelter.careAddr,
                shelter.orgNm
            )
        }
    }
}
