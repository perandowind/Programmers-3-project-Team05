package com.team05.petmeeting.domain.adoption.controller

import com.team05.petmeeting.domain.adoption.dto.AdoptionApplyRes
import com.team05.petmeeting.domain.adoption.dto.AdoptionDetailRes
import com.team05.petmeeting.domain.adoption.dto.AdoptionReviewReq
import com.team05.petmeeting.domain.adoption.entity.AdoptionStatus
import com.team05.petmeeting.domain.adoption.errorcode.AdoptionErrorCode
import com.team05.petmeeting.domain.adoption.service.AdoptionAdminService
import com.team05.petmeeting.global.exception.BusinessException
import com.team05.petmeeting.global.security.filter.JwtAuthenticationFilter
import com.team05.petmeeting.global.security.userdetails.CustomUserDetails
import com.team05.petmeeting.global.security.util.JwtUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(AdoptionAdminController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AdoptionAdminControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var adoptionAdminService: AdoptionAdminService

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
    @DisplayName("보호소 관리자 입양 신청 목록 조회 성공")
    fun getManagedShelterApplications() {
        val response = AdoptionApplyRes(
            10L,
            AdoptionStatus.Processing,
            AdoptionApplyRes.AnimalInfo("A-001", "믹스견", "담당보호소", "보호소장"),
        )
        whenever(adoptionAdminService.getManagedShelterApplications(USER_ID, CARE_REG_NO))
            .thenReturn(listOf(response))

        mockMvc.perform(
            get("/api/v1/adoptions/admin/shelters/{careRegNo}/applications", CARE_REG_NO)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].applicationId").value(10L))
            .andExpect(jsonPath("$[0].status").value("Processing"))
            .andExpect(jsonPath("$[0].animalInfo.desertionNo").value("A-001"))
            .andExpect(jsonPath("$[0].animalInfo.careNm").value("담당보호소"))

        verify(adoptionAdminService).getManagedShelterApplications(USER_ID, CARE_REG_NO)
    }

    @Test
    @DisplayName("보호소 관리자 입양 신청 상세 조회 성공")
    fun getManagedShelterApplicationDetail() {
        val applicationId = 10L
        val createdAt = LocalDateTime.of(2026, 4, 22, 10, 0)
        val response = AdoptionDetailRes(
            applicationId,
            AdoptionStatus.Processing,
            "입양하고 싶습니다.",
            createdAt,
            null,
            null,
            "010-1234-5678",
            AdoptionDetailRes.AnimalInfo(
                "A-001",
                "특이사항 없음",
                "담당보호소",
                "보호소장",
                "010-0000-0000",
                "서울시",
            ),
        )
        whenever(adoptionAdminService.getManagedShelterApplicationDetail(USER_ID, CARE_REG_NO, applicationId))
            .thenReturn(response)

        mockMvc.perform(
            get("/api/v1/adoptions/admin/shelters/{careRegNo}/applications/{applicationId}", CARE_REG_NO, applicationId)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.applicationId").value(applicationId))
            .andExpect(jsonPath("$.status").value("Processing"))
            .andExpect(jsonPath("$.applyReason").value("입양하고 싶습니다."))
            .andExpect(jsonPath("$.applyTel").value("010-1234-5678"))
            .andExpect(jsonPath("$.animalInfo.desertionNo").value("A-001"))
            .andExpect(jsonPath("$.animalInfo.careNm").value("담당보호소"))

        verify(adoptionAdminService).getManagedShelterApplicationDetail(USER_ID, CARE_REG_NO, applicationId)
    }

    @Test
    @DisplayName("보호소 관리자가 아니면 에러 응답을 반환한다")
    fun getManagedShelterApplications_unauthorizedShelter() {
        whenever(adoptionAdminService.getManagedShelterApplications(USER_ID, CARE_REG_NO))
            .thenThrow(BusinessException(AdoptionErrorCode.UNAUTHORIZED_SHELTER))

        mockMvc.perform(
            get("/api/v1/adoptions/admin/shelters/{careRegNo}/applications", CARE_REG_NO)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("AD-003"))
            .andExpect(jsonPath("$.message").value("해당 보호소의 관리자가 아닙니다."))
    }

    @Test
    @DisplayName("보호소 관리자 입양 신청 상태 검토 성공")
    fun reviewApplication() {
        val applicationId = 10L
        val response = AdoptionDetailRes(
            applicationId,
            AdoptionStatus.Approved,
            "입양하고 싶습니다.",
            LocalDateTime.of(2026, 4, 22, 10, 0),
            LocalDateTime.of(2026, 4, 22, 11, 0),
            null,
            "010-1234-5678",
            AdoptionDetailRes.AnimalInfo(
                "A-001",
                "특이사항 없음",
                "담당보호소",
                "보호소장",
                "010-0000-0000",
                "서울시",
            ),
        )
        whenever(
            adoptionAdminService.reviewApplication(
                eq(USER_ID),
                eq(CARE_REG_NO),
                eq(applicationId),
                any(),
            ),
        ).thenReturn(response)

        mockMvc.perform(
            patch("/api/v1/adoptions/admin/shelters/{careRegNo}/applications/{applicationId}/review", CARE_REG_NO, applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"Approved","rejectionReason":null}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.applicationId").value(applicationId))
            .andExpect(jsonPath("$.status").value("Approved"))
            .andExpect(jsonPath("$.reviewedAt").isNotEmpty)
            .andExpect(jsonPath("$.rejectionReason").doesNotExist())

        verify(adoptionAdminService).reviewApplication(
            eq(USER_ID),
            eq(CARE_REG_NO),
            eq(applicationId),
            any(),
        )
    }

    private fun auth(): UsernamePasswordAuthenticationToken {
        val userDetails = CustomUserDetails(USER_ID, listOf())
        return UsernamePasswordAuthenticationToken(userDetails, null, listOf())
    }

    companion object {
        private const val USER_ID = 1L
        private const val CARE_REG_NO = "S-001"
    }
}
