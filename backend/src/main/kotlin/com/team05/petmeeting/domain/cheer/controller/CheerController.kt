package com.team05.petmeeting.domain.cheer.controller

import com.team05.petmeeting.domain.cheer.dto.CheerRes
import com.team05.petmeeting.domain.cheer.dto.CheerStatusDto
import com.team05.petmeeting.domain.cheer.service.CheerService
import com.team05.petmeeting.global.security.userdetails.CustomUserDetails
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
@Tag(name = "CheerController", description = "응원 API")
class CheerController(
    private val cheerService: CheerService
) {

    @GetMapping("/cheers/today")
    @Operation(summary = "잔여 응원 횟수 조회")
    fun getTodaysCheers(
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<CheerStatusDto> {
        // Service 호출
        val status = cheerService.getTodaysStatus(userDetails.userId)
        return ResponseEntity.ok(status)
    }

    @PostMapping("/animals/{animalId}/cheers")
    @Operation(summary = "응원 부여")
    fun cheerAnimal(
        @PathVariable animalId: Long,
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<CheerRes> {
        val cheerRes = cheerService.cheerAnimal(userDetails.userId, animalId)

        return ResponseEntity.ok(cheerRes)
    }

}
