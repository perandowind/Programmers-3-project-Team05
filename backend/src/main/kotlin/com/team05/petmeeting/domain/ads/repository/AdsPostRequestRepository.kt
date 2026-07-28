package com.team05.petmeeting.domain.ads.repository

import com.team05.petmeeting.domain.ads.entity.AdsPostRequest
import org.springframework.data.jpa.repository.JpaRepository

interface AdsPostRequestRepository : JpaRepository<AdsPostRequest, Long> {
    fun findByShelter_CareRegNo(careRegNo: String): List<AdsPostRequest>
}
