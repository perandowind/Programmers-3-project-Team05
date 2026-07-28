package com.team05.petmeeting.domain.cheer.controller

import com.team05.petmeeting.domain.animal.errorcode.AnimalErrorCode
import com.team05.petmeeting.domain.cheer.dto.CheerRes
import com.team05.petmeeting.domain.cheer.dto.CheerStatusDto
import com.team05.petmeeting.domain.cheer.errorcode.CheerErrorCode
import com.team05.petmeeting.domain.cheer.service.CheerService
import com.team05.petmeeting.domain.user.errorcode.UserErrorCode
import com.team05.petmeeting.global.exception.BusinessException
import com.team05.petmeeting.global.security.handler.JwtAuthenticationEntryPoint
import com.team05.petmeeting.global.security.userdetails.CustomUserDetails
import com.team05.petmeeting.global.security.util.JwtUtil
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers


@WebMvcTest(CheerController::class)
@ActiveProfiles("test")
class CheerControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc
    @MockitoBean
    lateinit var cheerService: CheerService
    // 추가: 보안 필터가 요구하는 JwtUtil을 가짜 빈으로 등록
    @MockitoBean
    lateinit var jwtUtil: JwtUtil
    @MockitoBean
    lateinit var jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint

    private val userId = 1L // 필드로 선언하면 헬퍼 메서드에서 공유 가능

    // 인증 객체 생성 헬퍼
    private fun auth(): UsernamePasswordAuthenticationToken {
        // CustomUserDetails 생성자 구조에 따라 (userId, authorities) 주입
        val userDetails = CustomUserDetails(userId, mutableListOf<GrantedAuthority>())
        return UsernamePasswordAuthenticationToken(userDetails, null, mutableListOf<GrantedAuthority>())
    }

    @Test
    @DisplayName("잔여 응원 횟수 조회 - 성공")
    @Throws(Exception::class)
    fun getTodaysCheers_success() {
        // given
        val statusDto = CheerStatusDto(2, 3, "2026-04-23T00:00:00")
        BDDMockito.given<CheerStatusDto>(cheerService.getTodaysStatus(userId)).willReturn(statusDto)

        // when & then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/cheers/today")
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth())) // 이 부분이 핵심!
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.usedToday").value(2))
    }

    @Test
    @WithMockUser // 기본적으로 'ROLE_USER' 권한을 가진 가짜 사용자를 세팅해줍니다.
    @DisplayName("응원 부여 - 성공")
    @Throws(Exception::class)
    fun cheerAnimal_success() {
        // given
        val animalId = 100L
        val res = CheerRes(animalId, 10, 36.5, 4)
        BDDMockito.given<CheerRes>(cheerService.cheerAnimal(userId, animalId)).willReturn(res)

        // when & then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/animals/{animalId}/cheers", animalId)
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth())) // 재사용
                .with(SecurityMockMvcRequestPostProcessors.csrf()) // csrf() 주입
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.animalId").value(animalId))
    }

    @Test
    @DisplayName("응원 부여 실패 - 존재하지 않는 사용자")
    @Throws(Exception::class)
    fun cheerAnimal_fail_userNotFound() {
        // given
        val animalId = 100L
        // 서비스에서 USER_NOT_FOUND 예외를 던지도록 설정
        BDDMockito.given<CheerRes>(cheerService.cheerAnimal(userId, animalId))
            .willThrow(BusinessException(UserErrorCode.USER_NOT_FOUND))

        // when & then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/animals/{animalId}/cheers", animalId)
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth()))
                .with(SecurityMockMvcRequestPostProcessors.csrf()) // csrf() 주입
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound()) // 404 에러 (UserErrorCode 설정에 따름)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("U-004")) // 실제 프로젝트의 에러코드 값으로 수정
    }

    @Test
    @DisplayName("응원 부여 실패 - 존재하지 않는 동물")
    @Throws(Exception::class)
    fun cheerAnimal_fail_animalNotFound() {
        // given
        val animalId = 999L // 존재하지 않는 ID 가정
        BDDMockito.given<CheerRes>(cheerService.cheerAnimal(userId, animalId))
            .willThrow(BusinessException(AnimalErrorCode.ANIMAL_NOT_FOUND))

        // when & then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/animals/{animalId}/cheers", animalId)
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth()))
                .with(SecurityMockMvcRequestPostProcessors.csrf()) // csrf() 주입
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("A-001")) // 실제 프로젝트의 에러코드 값으로 수정
    }

    @Test
    @DisplayName("응원 부여 실패 - 일일 응원 제한 초과")
    @Throws(Exception::class)
    fun cheerAnimal_fail_limitExceeded() {
        // given
        val animalId = 100L
        BDDMockito.given<CheerRes>(cheerService.cheerAnimal(userId, animalId))
            .willThrow(BusinessException(CheerErrorCode.DAILY_CHEER_LIMIT_EXCEEDED))

        // when & then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/animals/{animalId}/cheers", animalId)
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth()))
                .with(SecurityMockMvcRequestPostProcessors.csrf()) // csrf() 주입
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest()) // 400 에러
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("CH-001")) // 실제 프로젝트의 에러코드 값으로 수정
    }
}