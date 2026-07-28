package com.team05.petmeeting.domain.campaign.controller

import com.team05.petmeeting.domain.campaign.dto.CampaignCreateReq
import com.team05.petmeeting.domain.campaign.dto.CampaignCreateRes
import com.team05.petmeeting.domain.campaign.dto.CampaignDetailRes
import com.team05.petmeeting.domain.campaign.dto.CampaignRes
import com.team05.petmeeting.domain.campaign.service.CampaignService
import com.team05.petmeeting.global.security.userdetails.CustomUserDetails
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class CampaignController(private val campaignService: CampaignService) {

    @Operation(summary = "현재 진행 캠페인 전체 조회")
    @GetMapping("/campaigns")
    fun getCampaigns(): ResponseEntity<CampaignRes> {
        return ResponseEntity.ok(campaignService.allCampaigns)
    }

    @Operation(summary = "보호소 캠페인 생성")
    @PostMapping("/shelters/{shelterId}/campaign")
    fun createCampaign(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable shelterId: String,
        @Valid @RequestBody req: CampaignCreateReq
    ): ResponseEntity<CampaignCreateRes> {
        return ResponseEntity.ok(campaignService.createCampaign(shelterId, userDetails.userId, req))
    }

    @Operation(summary = "보호소 현재 진행 캠페인 전체 조회")
    @GetMapping("/shelters/{shelterId}/campaign")
    fun getCampaign(
        @PathVariable shelterId: String
    ): ResponseEntity<CampaignDetailRes> {
        return ResponseEntity.ok(campaignService.getCampaign(shelterId))
    }

    @Operation(summary = "캠페인 상태 변경 (진행 중인 캠페인 종료)")
    @PatchMapping("/campaigns/{campaignId}/status")
    fun closeCampaign(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable campaignId: Long
    ): ResponseEntity<Void> {
        campaignService.closeCampaign(userDetails.userId, campaignId)
        return ResponseEntity.noContent().build()
    }
}