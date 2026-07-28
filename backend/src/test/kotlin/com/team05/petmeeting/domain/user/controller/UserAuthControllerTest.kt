package com.team05.petmeeting.domain.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.team05.petmeeting.domain.user.dto.auth.emailsignup.EmailSignupReq
import com.team05.petmeeting.domain.user.dto.auth.emailstart.EmailStartReq
import com.team05.petmeeting.domain.user.dto.auth.emailstart.EmailStartRes
import com.team05.petmeeting.domain.user.dto.auth.emailstart.EmailStartRes.NextStep
import com.team05.petmeeting.domain.user.dto.auth.emailverify.EmailVerifyReq
import com.team05.petmeeting.domain.user.dto.auth.login.AccessTokenRes
import com.team05.petmeeting.domain.user.dto.auth.login.LoginAndRefreshRes
import com.team05.petmeeting.domain.user.dto.auth.login.local.EmailLoginReq
import com.team05.petmeeting.domain.user.service.UserAuthService
import com.team05.petmeeting.global.security.filter.JwtAuthenticationFilter
import com.team05.petmeeting.global.security.test.WithCustomUser
import com.team05.petmeeting.global.security.util.JwtUtil
import com.team05.petmeeting.global.security.util.RefreshTokenUtil
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WebMvcTest(UserAuthController::class)
@WithCustomUser(userId = 100L)
class UserAuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val objectMapper = ObjectMapper()

    @MockitoBean
    private lateinit var userAuthService: UserAuthService

    @MockitoBean
    private lateinit var refreshTokenUtil: RefreshTokenUtil

    @MockitoBean
    private lateinit var jwtUtil: JwtUtil

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @Test
    @DisplayName("이메일 시작 - 성공")
    fun startEmail() {
        val req = EmailStartReq("test@test.com")
        val res = EmailStartRes(true, NextStep.SIGNUP_WITH_OTP)

        `when`(userAuthService.startEmailFlow(anyString()))
            .thenReturn(res)

        mockMvc.perform(
            post("/api/v1/auth/email/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.exists").value(true))
    }

    @Test
    @DisplayName("OTP 발송 - 성공")
    fun sendOtp() {
        val req = EmailStartReq("test@test.com")

        mockMvc.perform(
            post("/api/v1/auth/email/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        )
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("이메일 인증 - 성공")
    fun verifyEmail() {
        val req = EmailVerifyReq("test@test.com", "123456")

        `when`(userAuthService.verifyOtp(anyString(), anyString()))
            .thenReturn("verify-token")

        mockMvc.perform(
            post("/api/v1/auth/email/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.verifyToken").value("verify-token"))
    }

    @Test
    @DisplayName("이메일 회원가입 - 성공")
    fun signup() {
        val req = EmailSignupReq(
            UUID.randomUUID().toString(),
            "Password12!",
            "nickname",
            "realname",
        )
        val accessToken = AccessTokenRes("Bearer", "access-token")
        val result = LoginAndRefreshRes("refreshToken", accessToken)

        `when`(userAuthService.signupAndLoginWithEmail(anyOr(req)))
            .thenReturn(result)

        mockMvc.perform(
            post("/api/v1/auth/email/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.accessToken").value("access-token"))
    }

    @Test
    @DisplayName("이메일 로그인 - 성공")
    fun login() {
        val req = EmailLoginReq("test@test.com", "Password12!")
        val accessToken = AccessTokenRes("Bearer", "access-token")
        val result = LoginAndRefreshRes("refershToken", accessToken)

        `when`(userAuthService.loginWithEmail(anyString(), anyString()))
            .thenReturn(result)

        mockMvc.perform(
            post("/api/v1/auth/email/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("access-token"))
    }

    @Test
    @DisplayName("로그아웃 - 성공")
    fun logout() {
        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("토큰 재발급 - 성공")
    fun refresh() {
        val accessToken = AccessTokenRes("Bearer", "new-access")
        val result = LoginAndRefreshRes("refreshToken", accessToken)

        `when`(userAuthService.refresh(anyOr(mock(HttpServletRequest::class.java))))
            .thenReturn(result)

        mockMvc.perform(post("/api/v1/auth/refresh"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("new-access"))
    }

    @Test
    @DisplayName("탈퇴 - 성공")
    fun withdraw() {
        doNothing().`when`(userAuthService).withdraw(anyLong())

        mockMvc.perform(delete("/api/v1/auth/withdraw"))
            .andExpect(status().isNoContent)
    }

    private inline fun <reified T : Any> anyOr(value: T): T =
        any(T::class.java) ?: value
}
