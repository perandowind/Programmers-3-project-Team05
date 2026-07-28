package com.team05.petmeeting.domain.shelter.controller;

import com.team05.petmeeting.domain.shelter.dto.ShelterRes;
import com.team05.petmeeting.domain.shelter.errorcode.ShelterErrorCode;
import com.team05.petmeeting.domain.shelter.service.ShelterService;
import com.team05.petmeeting.global.exception.BusinessException;
import com.team05.petmeeting.global.security.test.WithCustomUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithCustomUser(userId = 100L)
class ShelterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShelterService shelterService;

    // 보호소 조회 성공
    @Test
    void getShelter_success() throws Exception {
        ShelterRes res = new ShelterRes(
                "shelter-001",
                "테스트보호소",
                "010-0000-0000",
                "서울시 테스트구",
                "테스트기관"
        );
        when(shelterService.getShelter("shelter-001")).thenReturn(res);

        mockMvc.perform(get("/api/v1/shelters/shelter-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shelterId").value("shelter-001"))
                .andExpect(jsonPath("$.careNm").value("테스트보호소"));
    }

    // 보호소 조회 실패 - 존재하지 않는 보호소 (404)
    @Test
    void getShelter_fail_notFound() throws Exception {
        when(shelterService.getShelter("invalid-id"))
                .thenThrow(new BusinessException(ShelterErrorCode.SHELTER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/shelters/invalid-id"))
                .andExpect(status().isNotFound());
    }

    // 비로그인 접근 시 200 (보호소 조회는 permitAll)
    @Test
    @WithAnonymousUser
    void getShelter_anonymous_success() throws Exception {
        ShelterRes res = new ShelterRes(
                "shelter-001",
                "테스트보호소",
                "010-0000-0000",
                "서울시 테스트구",
                "테스트기관"
        );
        when(shelterService.getShelter("shelter-001")).thenReturn(res);

        mockMvc.perform(get("/api/v1/shelters/shelter-001"))
                .andExpect(status().isOk());
    }
}