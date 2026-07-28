package com.team05.petmeeting.domain.donation.controller

import com.team05.petmeeting.domain.donation.dto.CompleteReq
import com.team05.petmeeting.domain.donation.dto.CompleteRes
import com.team05.petmeeting.domain.donation.dto.PrepareReq
import com.team05.petmeeting.domain.donation.dto.PrepareRes
import com.team05.petmeeting.domain.donation.service.DonationService
import com.team05.petmeeting.global.security.userdetails.CustomUserDetails
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/donations")
class DonationController(private val donationService: DonationService) {
    @Operation(summary = "결제 준비")
    @PostMapping("/prepare")
    fun prepareDonation(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody req: PrepareReq
    ): ResponseEntity<PrepareRes> {
        val res = donationService.prepare(userDetails.userId, req)
        return ResponseEntity.ok(res)
    }

    @Operation(summary = "결제 완료")
    @PostMapping("/complete")
    suspend fun completeDonation(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody req: CompleteReq
    ): ResponseEntity<CompleteRes> {
        val res = donationService.donate(userDetails.userId, req)
        return ResponseEntity.ok(res)
    }

    @Operation(summary = "웹훅 처리")
    @PostMapping("/webhook")
    suspend fun handleWebhook(
        @RequestBody body: String,
        @RequestHeader("webhook-id") webhookId: String,
        @RequestHeader("webhook-signature") webhookSignature: String,
        @RequestHeader("webhook-timestamp") webhookTimestamp: String
    ): ResponseEntity<Unit> {
        donationService.handleWebhook(body, webhookId, webhookSignature, webhookTimestamp)
        return ResponseEntity.ok().build()
    }
}
