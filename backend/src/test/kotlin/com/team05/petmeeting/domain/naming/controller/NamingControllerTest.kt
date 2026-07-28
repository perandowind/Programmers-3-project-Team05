package com.team05.petmeeting.domain.naming.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.team05.petmeeting.domain.naming.dto.*
import com.team05.petmeeting.domain.naming.dto.BadWordListRes.BadWordDto
import com.team05.petmeeting.domain.naming.dto.NameCandidateRes.CandidateDto
import com.team05.petmeeting.domain.naming.service.NamingService
import com.team05.petmeeting.global.security.filter.JwtAuthenticationFilter
import com.team05.petmeeting.global.security.userdetails.CustomUserDetails
import com.team05.petmeeting.global.security.util.JwtUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDateTime

@WebMvcTest(NamingController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
internal class NamingControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    private val objectMapper = ObjectMapper()

    @MockitoBean
    lateinit var namingService: NamingService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @BeforeEach
    fun setUp() {
        val userDetails = CustomUserDetails(1L, listOf<GrantedAuthority>(SimpleGrantedAuthority("ROLE_USER")))
        val auth = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
        SecurityContextHolder.getContext().authentication = auth
    }

    private fun createTestUser(): CustomUserDetails {
        return CustomUserDetails(1L, listOf<GrantedAuthority>(SimpleGrantedAuthority("ROLE_USER")))
    }

    @Test
    @DisplayName("이름 후보 조회 테스트")
    @Throws(Exception::class)
    fun getNameCandidatesTest() {
        // given
        val animalId = 1L
        val response = NameCandidateRes(
            animalId, "초코",
            listOf<CandidateDto>(CandidateDto(1L, 1L, "바둑이", "유저1", 10, false)),
            1
        )
        BDDMockito.given<NameCandidateRes>(
            namingService.getCandidates(
                ArgumentMatchers.eq<Long>(animalId),
                ArgumentMatchers.anyLong()
            )
        ).willReturn(response)

        // when
        val resultActions = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/naming/animals/{animalId}/candidates", animalId)
        )

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.animalId").value(animalId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.candidateDtoList[0].proposedName").value("바둑이"))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("새로운 이름 제안 테스트")
    @Throws(Exception::class)
    fun proposeNameTest() {
        // given
        val animalId = 1L
        val memberId = 1L
        val proposedName = "코코"
        val request = NameProposalReq(proposedName)
        val response = NameProposalRes(1L, proposedName)

        BDDMockito.given<NameProposalRes>(
            namingService.proposeName(
                animalId,
                memberId,
                proposedName
            )
        ).willReturn(response)

        // when
        val resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/naming/animals/{animalId}/propose", animalId)

                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.proposedName").value(proposedName))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("기존 이름 투표 테스트")
    @Throws(Exception::class)
    fun voteNameTest() {
        // given
        val candidateId = 1L
        Mockito.doNothing().`when`(namingService)
            .vote(ArgumentMatchers.eq(candidateId), ArgumentMatchers.anyLong())

        // when
        val resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/naming/candidates/{candidateId}/vote", candidateId)
        )

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("금칙어 목록 조회 테스트")
    @Throws(Exception::class)
    fun getBadWordsTest() {
        // given
        val response = BadWordListRes(
            listOf<BadWordDto>(BadWordDto(1L, "나쁜말", LocalDateTime.now().toString())),
            1
        )
        BDDMockito.given<BadWordListRes>(namingService.getBadWords()).willReturn(response)

        // when
        val resultActions = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/naming/admin/badwords")
        )

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.badWords[0].word").value("나쁜말"))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("금칙어 추가 테스트")
    @Throws(Exception::class)
    fun addBadWordTest() {
        // given
        val word = "비속어"
        val response = BadWordAddRes(1L, word, LocalDateTime.now())

        BDDMockito.given<BadWordAddRes>(namingService.addBadWord(word))
            .willReturn(response)

        // when
        val resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/naming/admin/badwords")

                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(object : HashMap<String, String>() {
                    init {
                        put("badWord", "비속어")
                    }
                }))
        )

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.badWord").value(word))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("금칙어 삭제 테스트")
    @Throws(Exception::class)
    fun deleteBadWordTest() {
        // given
        val badwordId = 1L
        Mockito.doNothing().`when`(namingService).deleteBadWord(badwordId)

        // when
        val resultActions = mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/naming/admin/badwords/{badwordId}", badwordId)
        )

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.status().isNoContent())
            .andDo(MockMvcResultHandlers.print())
    }
}
