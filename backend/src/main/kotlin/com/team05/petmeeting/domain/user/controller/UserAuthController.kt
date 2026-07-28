package com.team05.petmeeting.domain.user.controller

import com.team05.petmeeting.domain.user.dto.auth.emailsignup.EmailSignupReq
import com.team05.petmeeting.domain.user.dto.auth.emailstart.EmailStartReq
import com.team05.petmeeting.domain.user.dto.auth.emailstart.EmailStartRes
import com.team05.petmeeting.domain.user.dto.auth.emailverify.EmailVerifyReq
import com.team05.petmeeting.domain.user.dto.auth.emailverify.EmailVerifyRes
import com.team05.petmeeting.domain.user.dto.auth.login.AccessTokenRes
import com.team05.petmeeting.domain.user.dto.auth.login.LoginAndRefreshRes
import com.team05.petmeeting.domain.user.dto.auth.login.local.EmailLoginReq
import com.team05.petmeeting.domain.user.service.UserAuthService
import com.team05.petmeeting.global.security.userdetails.CustomUserDetails
import com.team05.petmeeting.global.security.util.RefreshTokenUtil
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Validated
class UserAuthController(
    private val userAuthService: UserAuthService,
    private val refreshTokenUtil: RefreshTokenUtil,
) {

    @PostMapping("/email/start")
    fun startEmail(
        @Valid @RequestBody request: EmailStartReq,
    ): ResponseEntity<EmailStartRes> =
        userAuthService.startEmailFlow(request.email)
            .let { ResponseEntity.ok(it) }

    @PostMapping("/email/send-otp")
    fun sendEmail(
        @Valid @RequestBody request: EmailStartReq,
    ): ResponseEntity<Void> =
        userAuthService.sendSignupOtp(request.email)
            .let { noContent() }

    @PostMapping("/email/verify")
    fun verifyEmail(
        @Valid @RequestBody request: EmailVerifyReq,
    ): ResponseEntity<EmailVerifyRes> =
        userAuthService.verifyOtp(request.email, request.code)
            .let(::EmailVerifyRes)
            .let { ResponseEntity.ok(it) }

    @PostMapping("/email/signup")
    fun signupWithEmail(
        @Valid @RequestBody request: EmailSignupReq,
        response: HttpServletResponse,
    ): ResponseEntity<AccessTokenRes> =
        userAuthService.signupAndLoginWithEmail(request)
            .withRefreshTokenCookie(response)
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @PostMapping("/email/login")
    fun loginWithEmail(
        @Valid @RequestBody request: EmailLoginReq,
        response: HttpServletResponse,
    ): ResponseEntity<AccessTokenRes> =
        userAuthService.loginWithEmail(request.email, request.password)
            .withRefreshTokenCookie(response)
            .let { ResponseEntity.ok(it) }

    @PostMapping("/logout")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Void> =
        userAuthService.logout(request)
            .also { refreshTokenUtil.delete(response) }
            .let { noContent() }

    @PostMapping("/refresh")
    fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<AccessTokenRes> =
        userAuthService.refresh(request)
            .withRefreshTokenCookie(response)
            .let { ResponseEntity.ok(it) }

    @DeleteMapping("/withdraw")
    fun withdraw(
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
        response: HttpServletResponse,
    ): ResponseEntity<Void> =
        userDetails.requireUserId()
            .also(userAuthService::withdraw)
            .also { refreshTokenUtil.delete(response) }
            .let { noContent() }

    private fun LoginAndRefreshRes.withRefreshTokenCookie(response: HttpServletResponse): AccessTokenRes =
        also { refreshTokenUtil.add(response, it.refreshToken) }
            .accessTokenRes

    private fun CustomUserDetails?.requireUserId(): Long =
        requireNotNull(this) { "인증된 사용자 정보가 없습니다." }
            .userId

    private fun noContent(): ResponseEntity<Void> =
        ResponseEntity.noContent().build()
}
