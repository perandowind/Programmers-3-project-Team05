package com.team05.petmeeting.domain.animal.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team05.petmeeting.domain.animal.config.AnimalSyncProperties;
import com.team05.petmeeting.domain.animal.dto.AnimalSyncRes;
import com.team05.petmeeting.domain.animal.service.AnimalSyncService;
import com.team05.petmeeting.global.security.filter.JwtAuthenticationFilter;
import com.team05.petmeeting.global.security.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnimalSyncController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
// AnimalSyncController의 동기화 엔드포인트가 AnimalSyncService에 요청 값을 올바르게 전달하는지 검증
class AnimalSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalSyncService animalSyncService;

    @MockitoBean
    private AnimalSyncProperties animalSyncProperties;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("유기동물 페이지 동기화 요청 성공")
    void syncAnimals() throws Exception {
        AnimalSyncRes response = new AnimalSyncRes("동기화 완료", 3, 120L);
        given(animalSyncService.fetchAndSaveAnimals(2, 30)).willReturn(response);

        mockMvc.perform(post("/api/v1/animals/sync")
                        .param("pageNo", "2")
                        .param("numOfRows", "30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("동기화 완료"))
                .andExpect(jsonPath("$.savedCount").value(3))
                .andExpect(jsonPath("$.elapsedMs").value(120L));

        verify(animalSyncService).fetchAndSaveAnimals(2, 30);
    }

    @Test
    @DisplayName("유기동물 페이지 동기화 요청 시 파라미터가 없으면 설정 기본값을 사용한다")
    void syncAnimals_usesConfiguredDefaults() throws Exception {
        AnimalSyncRes response = new AnimalSyncRes("동기화 완료", 1, 50L);
        given(animalSyncProperties.getDefaultPageNo()).willReturn(1);
        given(animalSyncProperties.getDefaultNumOfRows()).willReturn(10);
        given(animalSyncService.fetchAndSaveAnimals(1, 10)).willReturn(response);

        mockMvc.perform(post("/api/v1/animals/sync")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("동기화 완료"))
                .andExpect(jsonPath("$.savedCount").value(1))
                .andExpect(jsonPath("$.elapsedMs").value(50L));

        verify(animalSyncService).fetchAndSaveAnimals(1, 10);
    }

    @Test
    @DisplayName("초기 월별 동기화 요청 성공")
    void syncInitialMonthly() throws Exception {
        AnimalSyncRes response = new AnimalSyncRes("초기 동기화 완료", 10, 300L);
        AnimalSyncProperties.Initial initial = new AnimalSyncProperties.Initial();
        initial.setNumOfRows(500);
        given(animalSyncProperties.getInitial()).willReturn(initial);
        given(animalSyncService.runInitialMonthlySync(500)).willReturn(response);

        mockMvc.perform(post("/api/v1/animals/sync/initial")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("초기 동기화 완료"))
                .andExpect(jsonPath("$.savedCount").value(10))
                .andExpect(jsonPath("$.elapsedMs").value(300L));

        verify(animalSyncService).runInitialMonthlySync(500);
    }

    @Test
    @DisplayName("수정일 기준 동기화 요청 성공")
    void syncByUpdatedDate() throws Exception {
        AnimalSyncRes response = new AnimalSyncRes("수정 동기화 완료", 5, 200L);
        given(animalSyncService.runUpdateSync(100)).willReturn(response);

        mockMvc.perform(post("/api/v1/animals/sync/update")
                        .param("numOfRows", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("수정 동기화 완료"))
                .andExpect(jsonPath("$.savedCount").value(5))
                .andExpect(jsonPath("$.elapsedMs").value(200L));

        verify(animalSyncService).runUpdateSync(100);
    }
}
