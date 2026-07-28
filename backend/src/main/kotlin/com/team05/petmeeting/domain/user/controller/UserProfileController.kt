package com.team05.petmeeting.domain.user.controller

import com.team05.petmeeting.domain.donation.service.DonationService
import com.team05.petmeeting.domain.user.dto.profile.*
import com.team05.petmeeting.domain.user.service.UserProfileService
import com.team05.petmeeting.global.security.userdetails.CustomUserDetails
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/me")
@Validated
class UserProfileController(
    private val userProfileService: UserProfileService,
    private val donationService: DonationService,
) {

    @PatchMapping("/nickname")
    fun nickname(
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
        @Valid @RequestBody req: NicknameReq,
    ): ResponseEntity<UserProfileRes> {
        val userId = userDetails.requireUserId()
        val res = userProfileService.modifyNickname(userId, req.nickname)
        return ResponseEntity.ok(res)
    }

    @PatchMapping("/profileImg")
    fun profileImg(
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
        @Valid @RequestBody req: ProfileImgReq,
    ): ResponseEntity<UserProfileRes> {
        val userId = userDetails.requireUserId()
        val res = userProfileService.modifyProfileImageUrl(userId, req.profileImageUrl)
        return ResponseEntity.ok(res)
    }

    @PatchMapping("/password")
    fun password(
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
        @Valid @RequestBody req: PasswordReq,
    ): ResponseEntity<Void> {
        val userId = userDetails.requireUserId()
        userProfileService.modifyPassword(userId, req.currentPassword, req.newPassword)
        return noContent()
    }

    @GetMapping
    fun getMyProfile(
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
    ): ResponseEntity<MyProfileDetailRes> {
        val userId = userDetails.requireUserId()
        val res = userProfileService.getMyProfileDetails(userId)
        return ResponseEntity.ok(res)
    }

    @GetMapping("/feeds")
    fun feeds(
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
    ): ResponseEntity<UserFeedRes> {
        val userId = userDetails.requireUserId()
        val res = userProfileService.getMyFeeds(userId)
        return ResponseEntity.ok(res)
    }

    @GetMapping("/comments/feeds")
    fun feedComments(
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
    ): ResponseEntity<UserFeedCommentRes> {
        val userId = userDetails.requireUserId()
        val res = userProfileService.getMyFeedComments(userId)
        return ResponseEntity.ok(res)
    }

    @GetMapping("/comments/animals")
    fun animalComments(
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
    ): ResponseEntity<UserAnimalCommentRes> {
        val userId = userDetails.requireUserId()
        val res = userProfileService.getMyAnimalComments(userId)
        return ResponseEntity.ok(res)
    }

    @GetMapping("/cheer-animals")
    fun animals(
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
    ): ResponseEntity<UserCheerAnimalRes> {
        val userId = userDetails.requireUserId()
        val res = userProfileService.getMyCheerAnimals(userId)
        return ResponseEntity.ok(res)
    }

    @GetMapping("/profile")
    fun getProfile(
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
    ): ResponseEntity<UserProfileRes> {
        val userId = userDetails.requireUserId()
        val res = userProfileService.getUserProfile(userId)
        return ResponseEntity.ok(res)
    }

    @GetMapping("/summary")
    fun getSummary(
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
    ): ResponseEntity<UserSummaryRes> {
        val userId = userDetails.requireUserId()
        val res = userProfileService.getUserSummary(userId)
        return ResponseEntity.ok(res)
    }

    @Operation(summary = "사용자 후원 목록 조회")
    @GetMapping("/donations")
    fun getDonations(
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
    ): ResponseEntity<UserDonationRes> {
        val userId = userDetails.requireUserId()
        val res = donationService.getMyDonations(userId)
        return ResponseEntity.ok(res)
    }

    private fun CustomUserDetails?.requireUserId(): Long =
        requireNotNull(this) { "인증된 사용자 정보가 없습니다." }
            .userId

    private fun noContent(): ResponseEntity<Void> =
        ResponseEntity.noContent().build()
}
