package com.team05.petmeeting.domain.ads.controller

import com.team05.petmeeting.domain.ads.dto.AdsPostRequestRes
import com.team05.petmeeting.domain.ads.entity.AdsPostStatus
import com.team05.petmeeting.domain.ads.errorCode.AdsErrorCode
import com.team05.petmeeting.domain.ads.service.AdsAdminService
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(AdsAdminController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
internal class AdsAdminControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var adsAdminService: AdsAdminService

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
    @DisplayName("보호소 관리자 광고 게시 요청 목록 조회 성공")
    fun getManagedShelterRequests() {
        whenever(adsAdminService.getManagedShelterRequests(USER_ID, CARE_REG_NO))
            .thenReturn(listOf(createResponse(REQUEST_ID, AdsPostStatus.Processing)))

        mockMvc.perform(
            get("/api/v1/ads/admin/shelters/{careRegNo}/requests", CARE_REG_NO)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(handler().handlerType(AdsAdminController::class.java))
            .andExpect(handler().methodName("getManagedShelterRequests"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].requestId").value(REQUEST_ID))
            .andExpect(jsonPath("$[0].status").value("Processing"))
            .andExpect(jsonPath("$[0].imageUrl").value("https://image-url.com/card.png"))
            .andExpect(jsonPath("$[0].caption").value("caption"))
            .andExpect(jsonPath("$[0].animalInfo.careRegNo").value(CARE_REG_NO))

        verify(adsAdminService).getManagedShelterRequests(USER_ID, CARE_REG_NO)
    }

    @Test
    @DisplayName("보호소 관리자 광고 게시 요청 상세 조회 성공")
    fun getManagedShelterRequestDetail() {
        whenever(adsAdminService.getManagedShelterRequestDetail(USER_ID, CARE_REG_NO, REQUEST_ID))
            .thenReturn(createResponse(REQUEST_ID, AdsPostStatus.Processing))

        mockMvc.perform(
            get("/api/v1/ads/admin/shelters/{careRegNo}/requests/{requestId}", CARE_REG_NO, REQUEST_ID)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(handler().handlerType(AdsAdminController::class.java))
            .andExpect(handler().methodName("getManagedShelterRequestDetail"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.requestId").value(REQUEST_ID))
            .andExpect(jsonPath("$.status").value("Processing"))
            .andExpect(jsonPath("$.animalInfo.desertionNo").value("A-001"))
            .andExpect(jsonPath("$.animalInfo.careNm").value("음성군 동물보호센터"))

        verify(adsAdminService).getManagedShelterRequestDetail(USER_ID, CARE_REG_NO, REQUEST_ID)
    }

    @Test
    @DisplayName("보호소 관리자 광고 게시 요청 승인 성공")
    fun reviewRequestApprove() {
        whenever(
            adsAdminService.reviewRequest(
                eq(USER_ID),
                eq(CARE_REG_NO),
                eq(REQUEST_ID),
                any(),
            ),
        ).thenReturn(createResponse(REQUEST_ID, AdsPostStatus.Published))

        mockMvc.perform(
            patch("/api/v1/ads/admin/shelters/{careRegNo}/requests/{requestId}/review", CARE_REG_NO, REQUEST_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"Approved","rejectionReason":null}"""),
        )
            .andExpect(handler().handlerType(AdsAdminController::class.java))
            .andExpect(handler().methodName("reviewRequest"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.requestId").value(REQUEST_ID))
            .andExpect(jsonPath("$.status").value("Published"))
            .andExpect(jsonPath("$.publishedAt").isNotEmpty)

        verify(adsAdminService).reviewRequest(
            eq(USER_ID),
            eq(CARE_REG_NO),
            eq(REQUEST_ID),
            any(),
        )
    }

    @Test
    @DisplayName("보호소 관리자 광고 게시 요청 사유 없이 거절 성공")
    fun reviewRequestReject() {
        whenever(
            adsAdminService.reviewRequest(
                eq(USER_ID),
                eq(CARE_REG_NO),
                eq(REQUEST_ID),
                any(),
            ),
        ).thenReturn(createResponse(REQUEST_ID, AdsPostStatus.Rejected))

        mockMvc.perform(
            patch("/api/v1/ads/admin/shelters/{careRegNo}/requests/{requestId}/review", CARE_REG_NO, REQUEST_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"Rejected"}"""),
        )
            .andExpect(handler().handlerType(AdsAdminController::class.java))
            .andExpect(handler().methodName("reviewRequest"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.requestId").value(REQUEST_ID))
            .andExpect(jsonPath("$.status").value("Rejected"))
            .andExpect(jsonPath("$.rejectionReason").doesNotExist())

        verify(adsAdminService).reviewRequest(
            eq(USER_ID),
            eq(CARE_REG_NO),
            eq(REQUEST_ID),
            any(),
        )
    }

    @Test
    @DisplayName("보호소 관리자 광고 게시 요청 재승인 요청 성공")
    fun reviewRequestMarkProcessing() {
        whenever(
            adsAdminService.reviewRequest(
                eq(USER_ID),
                eq(CARE_REG_NO),
                eq(REQUEST_ID),
                any(),
            ),
        ).thenReturn(createResponse(REQUEST_ID, AdsPostStatus.Processing))

        mockMvc.perform(
            patch("/api/v1/ads/admin/shelters/{careRegNo}/requests/{requestId}/review", CARE_REG_NO, REQUEST_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"Processing"}"""),
        )
            .andExpect(handler().handlerType(AdsAdminController::class.java))
            .andExpect(handler().methodName("reviewRequest"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.requestId").value(REQUEST_ID))
            .andExpect(jsonPath("$.status").value("Processing"))

        verify(adsAdminService).reviewRequest(
            eq(USER_ID),
            eq(CARE_REG_NO),
            eq(REQUEST_ID),
            any(),
        )
    }

    @Test
    @DisplayName("보호소 관리자가 아니면 에러 응답을 반환한다")
    fun getManagedShelterRequestsUnauthorizedShelter() {
        whenever(adsAdminService.getManagedShelterRequests(USER_ID, CARE_REG_NO))
            .thenThrow(BusinessException(AdsErrorCode.UNAUTHORIZED_SHELTER))

        mockMvc.perform(
            get("/api/v1/ads/admin/shelters/{careRegNo}/requests", CARE_REG_NO)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(handler().handlerType(AdsAdminController::class.java))
            .andExpect(handler().methodName("getManagedShelterRequests"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("ADS-003"))
            .andExpect(jsonPath("$.message").value("해당 보호소의 관리자가 아닙니다."))
    }

    private fun createResponse(
        requestId: Long,
        status: AdsPostStatus,
        rejectionReason: String? = null,
    ): AdsPostRequestRes =
        AdsPostRequestRes(
            requestId = requestId,
            status = status,
            imageUrl = "https://image-url.com/card.png",
            caption = "caption",
            createdAt = LocalDateTime.of(2026, 5, 19, 10, 0),
            reviewedAt = if (status == AdsPostStatus.Processing) null else LocalDateTime.of(2026, 5, 19, 11, 0),
            publishedAt = if (status == AdsPostStatus.Published) LocalDateTime.of(2026, 5, 19, 11, 5) else null,
            rejectionReason = rejectionReason,
            animalInfo = AdsPostRequestRes.AnimalInfo(
                desertionNo = "A-001",
                kindFullNm = "[개] 믹스견",
                age = "2026(년생)",
                sexCd = "M",
                careRegNo = CARE_REG_NO,
                careNm = "음성군 동물보호센터",
                specialMark = "활발함",
            ),
        )

    private fun auth(): UsernamePasswordAuthenticationToken {
        val userDetails = CustomUserDetails(USER_ID, listOf())
        return UsernamePasswordAuthenticationToken(userDetails, null, listOf())
    }

    companion object {
        private const val USER_ID = 1L
        private const val CARE_REG_NO = "343447202600001"
        private const val REQUEST_ID = 10L
    }
}
