package com.team05.petmeeting.domain.animal.repository

import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.animal.entity.Animal.Companion.builder
import com.team05.petmeeting.global.config.JpaAuditingConfig
import com.team05.petmeeting.global.config.QueryDslConfig
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import java.util.function.Predicate

@DataJpaTest
@Import(value = [QueryDslConfig::class, JpaAuditingConfig::class])
@ActiveProfiles("test")
internal class AnimalRepositoryTest {
    @Autowired
    lateinit var animalRepository: AnimalRepository

    @BeforeEach
    fun setUp() {
        animalRepository.saveAll<Animal>(
            listOf(
                builder()
                    .desertionNo("2024001")
                    .noticeNo("경남-진주-2024-001")
                    .upKindNm("개")
                    .processState("보호중")
                    .stateGroup(0) // 필수 필드 추가!
                    .totalCheerCount(10)
                    .weight("10.5(Kg)")
                    .careAddr("경상남도 진주시")
                    .specialMark("입질 있음")
                    .noticeEdt(java.time.LocalDate.of(2024, 12, 31))
                    .build(),
                builder()
                    .desertionNo("2024002")
                    .noticeNo("경남-창원-2024-002")
                    .upKindNm("개")
                    .processState("종료(입양)")
                    .stateGroup(1) // 필수 필드 추가!
                    .totalCheerCount(50)
                    .weight("5.0(Kg)")
                    .careAddr("경상남도 창원시")
                    .specialMark("매우 순함")
                    .noticeEdt(java.time.LocalDate.of(2024, 10, 10))
                    .build(),
                builder()
                    .desertionNo("2024003")
                    .noticeNo("서울-강남-2024-001")
                    .upKindNm("고양이")
                    .processState("보호중")
                    .stateGroup(0) // 필수 필드 추가!
                    .totalCheerCount(30)
                    .weight("3.0(Kg)")
                    .careAddr("서울특별시 강남구")
                    .specialMark("사람을 좋아함")
                    .noticeEdt(java.time.LocalDate.of(2024, 11, 1))
                    .build(),
                builder()
                    .desertionNo("2024004")
                    .noticeNo("서울-송파-2024-002")
                    .upKindNm("개")
                    .processState("보호중")
                    .stateGroup(0) // 필수 필드 추가!
                    .totalCheerCount(5)
                    .weight("8.5(Kg)")
                    .careAddr("서울특별시 송파구")
                    .specialMark("온순함")
                    .noticeEdt(java.time.LocalDate.of(2024, 12, 1))
                    .build()
            )
        )
    }

    @Test
    @DisplayName("지역 필터링(noticeNo.startsWith) 검증")
    fun filterByRegion() {
        // given
        val region = "경남" // AnimalRepositoryImpl에서 noticeNo.startsWith(region) 사용
        val pageable: Pageable = PageRequest.of(0, 10)

        // when
        val result: Page<Animal> = animalRepository.findAnimalsWithFilter(region, null, null, null, pageable)

        // then
        Assertions.assertThat(result.getContent()).hasSize(2)
        Assertions.assertThat(result.getContent())
            .allMatch(Predicate { a: Animal? -> a!!.noticeNo!!.startsWith("경남") })
    }

    @Test
    @DisplayName("축종 필터링(upKindNm) 검증")
    fun filterByKind() {
        // given
        val kind = "고양이"
        val pageable: Pageable = PageRequest.of(0, 10)

        // when
        val result: Page<Animal> = animalRepository.findAnimalsWithFilter(null, kind, null, null, pageable)

        // then
        Assertions.assertThat(result.getContent()).hasSize(1)
        Assertions.assertThat(result.getContent()[0].upKindNm).isEqualTo("고양이")
    }

    @Test
    @DisplayName("상태 그룹 필터링(stateGroup) 검증")
    fun filterByStateGroup() {
        // given
        val stateGroup = 1 // 종료 상태
        val pageable: Pageable = PageRequest.of(0, 10)

        // when
        val result: Page<Animal> = animalRepository.findAnimalsWithFilter(null, null, null, stateGroup, pageable)

        // then
        Assertions.assertThat(result.getContent()).hasSize(1)
        Assertions.assertThat(result.getContent()[0].processState).contains("종료")
    }

    @Test
    @DisplayName("페이징 및 정렬(응원수 기준) 검증")
    fun pagingAndSorting() {
        // given
        // totalCheerCount 내림차순 정렬, 페이지당 2개 조회
        val pageable: Pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "totalCheerCount"))

        // when
        val result: Page<Animal> = animalRepository.findAnimalsWithFilter(null, null, null, null, pageable)

        // then
        Assertions.assertThat(result.getContent()).hasSize(2)
        // 1위: 경남 개(50), 2위: 서울 고양이(30)
        Assertions.assertThat(result.getContent()[0].totalCheerCount).isEqualTo(50)
        Assertions.assertThat(result.getContent()[1].totalCheerCount).isEqualTo(30)
        Assertions.assertThat(result.totalElements).isEqualTo(4)
    }

    @Test
    @DisplayName("맞춤 추천 쿼리(findMatched) 검증 - 하이버네이트 6 캐스팅 및 페이징 정상 작동")
    fun testFindMatched() {
        // given
        // 2024004 (송파구 개): 8.5kg(중형견), 온순함(초보자용), 서울지역, 보호중 -> 매칭됨
        // 2024001 (진주시 개): 입질(초보자용 X), 경남지역 -> 매칭 실패
        val species = "개"
        val size = "중형 (어디서나 당당한 크기)"
        val region = "서울/경기/인천"
        val housing = "아파트/원룸 (실내 생활 위주)"
        val activity = "매일 산책 가능 (활동적인 편)"
        val experience = "처음 키워보는 초보 집사"
        val pageable: Pageable = PageRequest.of(0, 10)

        // when
        val result: Page<Animal> = animalRepository.findMatched(species, size, region, housing, activity, experience, pageable)

        // then
        Assertions.assertThat(result.content).hasSize(1)
        Assertions.assertThat(result.content[0].desertionNo).isEqualTo("2024004")
        Assertions.assertThat(result.content[0].weight).isEqualTo("8.5(Kg)")
    }
}