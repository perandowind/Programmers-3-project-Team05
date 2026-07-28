package com.team05.petmeeting.domain.adoption.controller

import com.team05.petmeeting.domain.adoption.dto.AdoptionApplyRes
import com.team05.petmeeting.domain.adoption.dto.AdoptionDetailRes
import com.team05.petmeeting.domain.adoption.dto.AdoptionReviewReq
import com.team05.petmeeting.domain.adoption.service.AdoptionAdminService
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
@RequestMapping("/api/v1/adoptions/admin")
class AdoptionAdminController(
    private val adoptionAdminService: AdoptionAdminService,
) {
    // 보호소 관리자가 담당 보호소에 접수된 입양 신청 목록을 조회한다.
    @GetMapping("/shelters/{careRegNo}/applications")
    fun getManagedShelterApplications(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable careRegNo: String,
    ): List<AdoptionApplyRes> =
        adoptionAdminService.getManagedShelterApplications(userDetails.userId, careRegNo)

    // 보호소 관리자가 담당 보호소의 입양 신청 상세 정보를 조회한다.
    @GetMapping("/shelters/{careRegNo}/applications/{applicationId}")
    fun getManagedShelterApplicationDetail(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable careRegNo: String,
        @PathVariable applicationId: Long,
    ): AdoptionDetailRes =
        adoptionAdminService.getManagedShelterApplicationDetail(userDetails.userId, careRegNo, applicationId)

    // 보호소 관리자가 담당 보호소의 입양 신청 상태를 검토한다.
    @PatchMapping("/shelters/{careRegNo}/applications/{applicationId}/review")
    fun reviewApplication(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable careRegNo: String,
        @PathVariable applicationId: Long,
        @Valid @RequestBody request: AdoptionReviewReq,
    ): AdoptionDetailRes =
        adoptionAdminService.reviewApplication(userDetails.userId, careRegNo, applicationId, request)
}
