package com.team05.petmeeting.domain.adoption.controller

import com.team05.petmeeting.domain.adoption.dto.AdoptionApplyReq
import com.team05.petmeeting.domain.adoption.dto.AdoptionApplyRes
import com.team05.petmeeting.domain.adoption.dto.AdoptionDetailRes
import com.team05.petmeeting.domain.adoption.entity.AdoptionStatus
import com.team05.petmeeting.domain.adoption.errorcode.AdoptionErrorCode
import com.team05.petmeeting.domain.adoption.service.AdoptionService
import com.team05.petmeeting.global.exception.BusinessException
import com.team05.petmeeting.global.security.filter.JwtAuthenticationFilter
import com.team05.petmeeting.global.security.userdetails.CustomUserDetails
import com.team05.petmeeting.global.security.util.JwtUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@WebMvcTest(AdoptionController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
// 일반 사용자 입장에서 입양 신청 목록, 상세, 제출, 취소 API를 검증한다.
class AdoptionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var adoptionService: AdoptionService

    @MockitoBean
    private lateinit var jwtUtil: JwtUtil

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @BeforeEach
    fun setUpAuthentication() {
        SecurityContextHolder.getContext().authentication = auth()
    }

    @AfterEach
    fun clearAuthentication() {
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("로그인 사용자의 입양 신청 목록 조회 성공")
    fun getMyAdoptions() {
        val response = AdoptionApplyRes(
            10L,
            AdoptionStatus.Processing,
            AdoptionApplyRes.AnimalInfo("A-001", "믹스견", "테스트 보호소", "보호소장"),
        )
        whenever(adoptionService.getMyAdoptions(USER_ID)).thenReturn(listOf(response))

        mockMvc.perform(
            get("/api/v1/adoptions/me")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].applicationId").value(10L))
            .andExpect(jsonPath("$[0].status").value("Processing"))
            .andExpect(jsonPath("$[0].animalInfo.desertionNo").value("A-001"))
            .andExpect(jsonPath("$[0].animalInfo.careNm").value("테스트 보호소"))

        verify(adoptionService).getMyAdoptions(USER_ID)
    }

    @Test
    @DisplayName("로그인 사용자의 입양 신청 상세 조회 성공")
    fun getApplicationDetail() {
        val applicationId = 10L
        val response = AdoptionDetailRes(
            applicationId,
            AdoptionStatus.Processing,
            "입양하고 싶습니다.",
            LocalDateTime.of(2026, 4, 23, 10, 0),
            null,
            null,
            "010-1234-5678",
            AdoptionDetailRes.AnimalInfo(
                "A-001",
                "특이사항 없음",
                "테스트 보호소",
                "보호소장",
                "010-0000-0000",
                "서울시",
            ),
        )
        whenever(adoptionService.getApplicationDetail(USER_ID, applicationId)).thenReturn(response)

        mockMvc.perform(
            get("/api/v1/adoptions/{applicationId}", applicationId)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.applicationId").value(applicationId))
            .andExpect(jsonPath("$.status").value("Processing"))
            .andExpect(jsonPath("$.applyReason").value("입양하고 싶습니다."))
            .andExpect(jsonPath("$.applyTel").value("010-1234-5678"))
            .andExpect(jsonPath("$.animalInfo.desertionNo").value("A-001"))
            .andExpect(jsonPath("$.animalInfo.careTel").value("010-0000-0000"))

        verify(adoptionService).getApplicationDetail(USER_ID, applicationId)
    }

    @Test
    @DisplayName("로그인 사용자의 입양 신청 제출 성공")
    fun applyApplication() {
        val animalId = 20L
        val response = AdoptionApplyRes(
            10L,
            AdoptionStatus.Processing,
            AdoptionApplyRes.AnimalInfo("A-001", "믹스견", "테스트 보호소", "보호소장"),
        )
        whenever(adoptionService.applyApplication(eq(USER_ID), eq(animalId), any()))
            .thenReturn(response)

        mockMvc.perform(
            post("/api/v1/adoptions/{animalId}", animalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "applyReason": "가족으로 맞이하고 싶습니다.",
                      "applyTel": "010-1234-5678"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.applicationId").value(10L))
            .andExpect(jsonPath("$.status").value("Processing"))
            .andExpect(jsonPath("$.animalInfo.desertionNo").value("A-001"))

        verify(adoptionService).applyApplication(eq(USER_ID), eq(animalId), any())
    }

    @Test
    @DisplayName("이미 신청한 동물은 중복 신청 에러를 반환한다")
    fun applyApplication_alreadyApplied() {
        whenever(adoptionService.applyApplication(eq(USER_ID), eq(20L), any()))
            .thenThrow(BusinessException(AdoptionErrorCode.ALREADY_APPLIED))

        mockMvc.perform(
            post("/api/v1/adoptions/{animalId}", 20L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "applyReason": "가족으로 맞이하고 싶습니다.",
                      "applyTel": "010-1234-5678"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("AD-002"))
            .andExpect(jsonPath("$.message").value("이미 입양 신청한 동물입니다."))
    }

    @Test
    @DisplayName("로그인 사용자의 입양 신청 취소 성공")
    fun cancelApplication() {
        val applicationId = 10L
        doNothing().`when`(adoptionService).cancelApplication(USER_ID, applicationId)

        mockMvc.perform(
            delete("/api/v1/adoptions/{applicationId}", applicationId)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isNoContent)

        verify(adoptionService).cancelApplication(USER_ID, applicationId)
    }

    private fun auth(): UsernamePasswordAuthenticationToken {
        val userDetails = CustomUserDetails(USER_ID, listOf())
        return UsernamePasswordAuthenticationToken(userDetails, null, listOf())
    }

    companion object {
        private const val USER_ID = 1L
    }
}
