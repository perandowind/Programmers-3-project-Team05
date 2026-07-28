package com.team05.petmeeting.domain.adoption.controller

import com.team05.petmeeting.domain.adoption.dto.AdoptionApplyReq
import com.team05.petmeeting.domain.adoption.dto.AdoptionApplyRes
import com.team05.petmeeting.domain.adoption.dto.AdoptionDetailRes
import com.team05.petmeeting.domain.adoption.service.AdoptionService
import com.team05.petmeeting.global.security.userdetails.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/adoptions")
class AdoptionController(
    private val adoptionService: AdoptionService,
) {
    // 로그인한 사용자의 입양 신청 목록을 조회한다.
    @GetMapping("/me")
    fun getMyAdoptions(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): List<AdoptionApplyRes> {
        val userId = userDetails.userId
        return adoptionService.getMyAdoptions(userId)
    }

    // 로그인한 사용자의 입양 신청 상세를 단건 조회한다.
    @GetMapping("/{applicationId}")
    fun getApplicationDetail(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable applicationId: Long,
    ): AdoptionDetailRes = adoptionService.getApplicationDetail(userDetails.userId, applicationId)

    // 로그인한 사용자가 특정 동물에 대한 입양 신청서를 제출한다.
    @PostMapping("/{animalId}")
    fun applyApplication(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable animalId: Long,
        @Valid @RequestBody request: AdoptionApplyReq,
    ): AdoptionApplyRes = adoptionService.applyApplication(userDetails.userId, animalId, request)

    // 로그인한 사용자가 본인의 입양 신청을 취소한다.
    @DeleteMapping("/{applicationId}")
    fun cancelApplication(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable applicationId: Long,
    ): ResponseEntity<Void> {
        adoptionService.cancelApplication(userDetails.userId, applicationId)
        return ResponseEntity.noContent().build()
    }
}
