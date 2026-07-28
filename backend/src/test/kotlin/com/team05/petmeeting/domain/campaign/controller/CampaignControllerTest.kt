package com.team05.petmeeting.domain.campaign.controller

import com.team05.petmeeting.domain.campaign.dto.CampaignCreateReq
import com.team05.petmeeting.domain.campaign.dto.CampaignCreateRes
import com.team05.petmeeting.domain.campaign.dto.CampaignDetailRes
import com.team05.petmeeting.domain.campaign.dto.CampaignRes
import com.team05.petmeeting.domain.campaign.enums.CampaignStatus
import com.team05.petmeeting.domain.campaign.errorcode.CampaignErrorCode
import com.team05.petmeeting.domain.campaign.service.CampaignService
import com.team05.petmeeting.global.exception.BusinessException
import com.team05.petmeeting.global.security.test.WithCustomUser
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import tools.jackson.databind.ObjectMapper

@WebMvcTest(
    controllers = [CampaignController::class],
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        OAuth2ClientAutoConfiguration::class //
                               ],
    excludeFilters = [ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = ["com\\.team05\\.petmeeting\\.global\\.security\\..*"]
    )]
)
@WithCustomUser(userId = 100L)
internal class CampaignControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var campaignService: CampaignService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun getCampaigns_success() {
        val res = CampaignRes(0, mutableListOf())
        Mockito.`when`(campaignService.allCampaigns).thenReturn(res)

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/campaigns")
            .with(csrf()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalCampaigns").value(0))
    }

//    @Test
//    @WithAnonymousUser
//    fun getCampaigns_anonymous_success() {
//        val res = CampaignRes(0, mutableListOf())
//        Mockito.`when`(campaignService.allCampaigns).thenReturn(res)
//
//        mockMvc.perform(
//            MockMvcRequestBuilders.get("/api/v1/campaigns")
//                .with(csrf())
//            )
//            .andExpect(MockMvcResultMatchers.status().isOk())
//    }

    @Test
    fun createCampaign_success() {
        val req = CampaignCreateReq("테스트 캠페인", "설명", 100000)
        val res = CampaignCreateRes(1L, "테스트 캠페인", 100000, CampaignStatus.ACTIVE)
        Mockito.`when`(campaignService.createCampaign("shelter-001", 100L, req)).thenReturn(res)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/shelters/shelter-001/campaign")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("테스트 캠페인"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.targetAmount").value(100000))
    }

    @Test
    @WithAnonymousUser
    fun createCampaign_fail_unauthorized() {
        val req = CampaignCreateReq("테스트 캠페인", "설명", 100000)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/shelters/shelter-001/campaign")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

    @Test
    fun createCampaign_fail_forbidden() {
        val req = CampaignCreateReq("테스트 캠페인", "설명", 100000)
        Mockito.`when`(campaignService.createCampaign("shelter-001", 100L, req))
            .thenThrow(BusinessException(CampaignErrorCode.UNAUTHORIZED_SHELTER))

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/shelters/shelter-001/campaign")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden())
    }

    @Test
    fun getCampaign_success() {
        val res = CampaignDetailRes(0, mutableListOf())
        Mockito.`when`(campaignService.getCampaign("shelter-001")).thenReturn(res)

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/shelters/shelter-001/campaign")
            .with(csrf()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.campaignCount").value(0))
    }

//    @Test
//    @WithAnonymousUser
//    fun getCampaign_anonymous_success() {
//        val res = CampaignDetailRes(0, mutableListOf())
//        Mockito.`when`(campaignService.getCampaign("shelter-001")).thenReturn(res)
//
//        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/shelters/shelter-001/campaign")
//            .with(csrf())
//        )
//            .andExpect(MockMvcResultMatchers.status().isOk())
//    }

    @Test
    fun closeCampaign_success() {
        Mockito.doNothing().`when`(campaignService).closeCampaign(100L, 1L)

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/campaigns/1/status")
            .with(csrf()))
            .andExpect(MockMvcResultMatchers.status().isNoContent())
    }

    @Test
    @WithAnonymousUser
    fun closeCampaign_fail_unauthorized() {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/campaigns/1/status")
            .with(csrf()))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

    @Test
    fun closeCampaign_fail_alreadyClosed() {
        Mockito.doThrow(BusinessException(CampaignErrorCode.CAMPAIGN_CLOSED))
            .`when`(campaignService).closeCampaign(100L, 1L)

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/campaigns/1/status")
            .with(csrf()))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
    }
}