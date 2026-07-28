package com.team05.petmeeting.domain.ads.controller

import com.team05.petmeeting.domain.ads.dto.AdsPostRequestRes
import com.team05.petmeeting.domain.ads.dto.AdsPostReviewReq
import com.team05.petmeeting.domain.ads.service.AdsAdminService
import com.team05.petmeeting.global.security.userdetails.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/ads/admin")
class AdsAdminController(
    private val adsAdminService: AdsAdminService,
) {
    @GetMapping("/shelters/{careRegNo}/requests")
    fun getManagedShelterRequests(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable careRegNo: String,
    ): List<AdsPostRequestRes> =
        adsAdminService.getManagedShelterRequests(userDetails.userId, careRegNo)

    @GetMapping("/shelters/{careRegNo}/requests/{requestId}")
    fun getManagedShelterRequestDetail(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable careRegNo: String,
        @PathVariable requestId: Long,
    ): AdsPostRequestRes =
        adsAdminService.getManagedShelterRequestDetail(userDetails.userId, careRegNo, requestId)

    @PatchMapping("/shelters/{careRegNo}/requests/{requestId}/review")
    fun reviewRequest(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable careRegNo: String,
        @PathVariable requestId: Long,
        @Valid @RequestBody request: AdsPostReviewReq,
    ): AdsPostRequestRes =
        adsAdminService.reviewRequest(userDetails.userId, careRegNo, requestId, request)
}
