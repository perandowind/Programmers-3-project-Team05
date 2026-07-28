package com.team05.petmeeting.domain.ads.controller

import com.team05.petmeeting.domain.ads.dto.AdsPostRequestRes
import com.team05.petmeeting.domain.ads.entity.AdsPostStatus
import com.team05.petmeeting.domain.ads.service.AdsService
import com.team05.petmeeting.global.security.handler.JwtAuthenticationEntryPoint
import com.team05.petmeeting.global.security.util.JwtUtil
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdsController::class)
@AutoConfigureMockMvc(addFilters = false)
internal class AdsControllerTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @MockitoBean
    private lateinit var adsService: AdsService

    @MockitoBean
    private lateinit var jwtUtil: JwtUtil

    @MockitoBean
    private lateinit var jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint

    @Test
    @DisplayName("Top N 동물 조회 성공")
    fun getTopAnimals_success() {
        Mockito.`when`(adsService.getTopAnimals(3)).thenReturn(emptyList())

        mvc.perform(
            get("/api/v1/ads/top-animals")
                .param("n", "3")
        )
            .andExpect(handler().handlerType(AdsController::class.java))
            .andExpect(handler().methodName("getTopAnimals"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.msg").value("Top N 동물 조회 성공"))
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    @DisplayName("카드뉴스 승인 요청 생성 성공")
    fun previewCardNews_success() {
        Mockito.`when`(adsService.createCardNewsPostRequests(1))
            .thenReturn(
                listOf(
                    AdsPostRequestRes(
                        requestId = 10L,
                        status = AdsPostStatus.Processing,
                        imageUrl = "https://image-url.com/card.png",
                        caption = "caption",
                        createdAt = null,
                        reviewedAt = null,
                        publishedAt = null,
                        rejectionReason = null,
                        animalInfo = AdsPostRequestRes.AnimalInfo(
                            desertionNo = "A-001",
                            kindFullNm = "[개] 믹스견",
                            age = "2026(년생)",
                            sexCd = "M",
                            careRegNo = "343447202600001",
                            careNm = "음성군 동물보호센터",
                            specialMark = "활발함",
                        ),
                    ),
                ),
            )

        mvc.perform(
            post("/api/v1/ads/card-news/preview")
                .param("n", "1")
        )
            .andExpect(handler().handlerType(AdsController::class.java))
            .andExpect(handler().methodName("previewCardNews"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.msg").value("카드뉴스 승인 요청 생성 성공"))
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.data[0].requestId").value(10))
            .andExpect(jsonPath("$.data[0].status").value("Processing"))
            .andExpect(jsonPath("$.data[0].imageUrl").value("https://image-url.com/card.png"))
            .andExpect(jsonPath("$.data[0].caption").value("caption"))
    }
}
