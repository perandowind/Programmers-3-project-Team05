package com.team05.petmeeting.domain.naming.controller

import com.team05.petmeeting.domain.naming.dto.*
import com.team05.petmeeting.domain.naming.service.NamingService
import com.team05.petmeeting.global.security.userdetails.CustomUserDetails
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/naming")
@Tag(name = "NamingController", description = "동물이름 투표(작명) API")
class NamingController(
    private val namingService: NamingService
) {
    // 제안/투표/후보조회 API

    @GetMapping("/animals/{animalId}/candidates")
    @Operation(summary = "이름 후보 조회", description = "득표순 내림차순 조회, 동표는 생성일순")
    fun getNameCandidates(
        @PathVariable animalId: Long,
        // 로그인 안 한 유저도 조회는 가능하므로 null 체크 필요
        @AuthenticationPrincipal userDetails: CustomUserDetails?
    ): ResponseEntity<NameCandidateRes> {
        val userId = userDetails?.userId
        val response = namingService.getCandidates(animalId, userId)
        return ResponseEntity.ok(response)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/qualified-candidates") // animalId 제거!
    @Operation(summary = "관리자용 확정 대기 전체 목록", description = "우리 보호소 동물 중 10표 이상 얻은 후보가 있는 모든 동물 조회")
    fun getAdminQualifiedCandidates(
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<List<NameCandidateRes>> {
        // 이제 새로고침해도 유저 정보(토큰)만 있으면 우리 보호소 애들 싹 다 가져옴
        val response = namingService.getAdminQualifiedList(userDetails.userId)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/animals/{animalId}/propose")
    @Operation(summary = "이름 작명(제안)", description = "새로운 이름을 제안합니다. 이미 존재하면 자동으로 투표 처리됩니다.")
    fun proposeName(
        @PathVariable animalId: Long,
        @Valid @RequestBody request: NameProposalReq,
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<NameProposalRes> {
        val response = namingService.proposeName(animalId, userDetails.userId, request.proposedName)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/candidates/{candidateId}/vote")
    @Operation(summary = "기존 이름 투표")
    fun voteName(
        @PathVariable candidateId: Long,
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<Void> {
        namingService.vote(candidateId, userDetails.userId)
        return ResponseEntity.ok().build()
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/candidates/{candidateId}/confirm")
    @Operation(summary = "관리자가 이름 확정", description = "해당 동물의 보호소 관리자가 최종 확정 실시")
    fun confirmNameByAdmin(
        @PathVariable candidateId: Long,
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<Void> {
        namingService.confirmName(candidateId, userDetails.userId)
        return ResponseEntity.ok().build()
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/badwords")
    @Operation(summary = "금칙어 조회")
    fun getBadWords(): ResponseEntity<BadWordListRes> {
        val response = namingService.getBadWords()
        return ResponseEntity.ok(response)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/badwords")
    @Operation(summary = "금칙어 추가")
    fun addBadWord(
        @Valid @RequestBody request: @Valid BadWordAddReq,
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<BadWordAddRes> {
        // 관리자 권한 검증 로직은 Security나 Service에서 수행
        val response = namingService.addBadWord(request.badWord)
        return ResponseEntity.ok(response)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/badwords/{badwordId}")
    @Operation(summary = "금칙어 삭제")
    fun deleteBadWord(
        @PathVariable badwordId: Long,
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<Void> {
        namingService.deleteBadWord(badwordId)
        return ResponseEntity.noContent().build() // 삭제 성공 시 204 No Content
    }
}
