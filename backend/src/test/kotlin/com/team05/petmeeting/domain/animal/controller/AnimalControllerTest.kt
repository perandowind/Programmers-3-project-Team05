package com.team05.petmeeting.domain.animal.controller

import com.team05.petmeeting.domain.animal.dto.AnimalRes
import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.animal.entity.Animal.Companion.builder
import com.team05.petmeeting.domain.animal.service.AnimalService
import com.team05.petmeeting.global.security.filter.JwtAuthenticationFilter
import com.team05.petmeeting.global.security.util.JwtUtil
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.domain.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDate

@WebMvcTest(AnimalController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
internal class AnimalControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var animalService: AnimalService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    // given
    val animal = builder()
        .desertionNo("12345")
        .processState("보호중")
        .stateGroup(0)
        .noticeNo("공고-1")
        .noticeEdt(LocalDate.now())
        .upKindNm("개")
        .kindFullNm("믹스견")
        .colorCd("흰색")
        .age("2023(년생)")
        .weight("5kg")
        .sexCd("M")
        .totalCheerCount(10)
        .build().apply {
            // 💡 범위 지정 함수 apply를 사용해서 객체 생성과 동시에
            // ReflectionTestUtils가 실행되도록 묶어주면 컴파일러의 널 추론 범위가 안전해집니다.
            ReflectionTestUtils.setField(this, "id", 1L)
        }

    @Test
    @DisplayName("유기동물 리스트 조회 테스트")
    @Throws(Exception::class)
    fun animalListTest() {
        // given
        val animalRes = AnimalRes(animal)

        // 컨트롤러의 @PageableDefault(sort = ["noticeEdt"], direction = Sort.Direction.ASC)와 일치시켜야함
        val pageable: Pageable = PageRequest.of(
            0,
            12,
            Sort.by(Sort.Direction.ASC, "noticeEdt")
        )
        val page: Page<AnimalRes> = PageImpl(listOf(animalRes), pageable, 1)

        BDDMockito.given<Page<AnimalRes>>(
            animalService.getAnimals(
                "서울",
                "개",
                "믹스견",
                0,
                pageable
                )
            ).willReturn(page)

        // when & then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/animals")
                .param("region", "서울")
                .param("kind", "개")
                .param("kindFullNm", "믹스견")
                .param("stateGroup", "0")
                .param("page", "0")
                .param("size", "12")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].desertionNo").value("12345"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].kindFullNm").value("믹스견"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("유기동물 상세 조회 테스트")
    @Throws(Exception::class)
    fun animalDetailTest() {
        // given
        val animalId = 1L

        BDDMockito.given<Animal>(animalService.findByAnimalId(animalId))
            .willReturn(animal)

        // when & then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/animals/{animalId}", animalId))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.desertionNo").value("12345"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.kindFullNm").value("믹스견"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalCheerCount").value(10))
            .andDo(MockMvcResultHandlers.print())
    }
}
