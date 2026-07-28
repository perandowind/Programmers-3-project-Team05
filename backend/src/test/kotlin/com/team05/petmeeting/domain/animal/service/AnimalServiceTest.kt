package com.team05.petmeeting.domain.animal.service

import com.team05.petmeeting.domain.animal.dto.AnimalRes
import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.animal.entity.Animal.Companion.builder
import com.team05.petmeeting.domain.animal.repository.AnimalRepository
import com.team05.petmeeting.domain.shelter.dto.ShelterCommand
import com.team05.petmeeting.domain.shelter.entity.Shelter.Companion.create
import com.team05.petmeeting.global.exception.BusinessException
import org.assertj.core.api.Assertions
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class AnimalServiceTest {
    @InjectMocks
    lateinit var animalService: AnimalService

    @Mock
    lateinit var animalRepository: AnimalRepository

    @Test
    @DisplayName("findByAnimalId 메서드는 존재하는 ID로 조회 시 Animal 엔티티를 반환한다.")
    fun findByAnimalId_success() {
        // given
        val animalId = 1L
        val animal = builder().desertionNo("12345").build()
        BDDMockito.given<Optional<Animal>?>(animalRepository.findById(animalId))
            .willReturn(Optional.of<Animal>(animal))

        // when
        val result = animalService.findByAnimalId(animalId)

        // then
        Assertions.assertThat(result.desertionNo).isEqualTo("12345")
    }

    @Test
    @DisplayName("findByAnimalId 메서드는 존재하지 않는 ID로 조회 시 BusinessException을 던진다.")
    fun findByAnimalId_fail_NotFound() {
        // given
        BDDMockito.given(animalRepository.findById(ArgumentMatchers.anyLong()))
            .willReturn(Optional.empty())

        // when & then
        Assertions.assertThatThrownBy(ThrowableAssert.ThrowingCallable { animalService.findByAnimalId(999L) })
            .isInstanceOf(BusinessException::class.java)
    }

    @Test
    @DisplayName("getAnimals 메서드는 필터 조건에 맞는 동물 목록을 페이징하여 반환한다.")
    fun getAnimals_success() {
        // given
        val pageable: Pageable = PageRequest.of(0, 10)
        val shelter = create(
            ShelterCommand(
                "CARE-123",
                "서울 보호소",
                "010-1234-5678",
                "서울시 강남구",
                "관리자",
                "서울시",
                LocalDateTime.now()
            )
        )

        val animal = builder()
            .desertionNo("123")
            .totalCheerCount(10)
            .shelter(shelter)
            .build()
        ReflectionTestUtils.setField(animal, "id", 1L)

        val animalPage: Page<Animal> = PageImpl<Animal>(listOf(animal), pageable, 1)

        // 야 mock repository야, 나중에 누가 findAnimalsWithFilter("서울","개",0,pageable) 호출하면 animalPage 돌려줘
        BDDMockito.given<Page<Animal>>(
            animalRepository.findAnimalsWithFilter(
                "서울",
                "개",
                "닥스훈트",
                0,
                pageable
            )
        )
            .willReturn(animalPage)

        // when
        val result: Page<AnimalRes> = animalService.getAnimals("서울", "개", "닥스훈트", 0, pageable)

        // then
        Assertions.assertThat(result.getContent()).hasSize(1)
        Assertions.assertThat(result.getContent()[0].desertionNo).isEqualTo("123")
    }

    @Test
    @DisplayName("getAnimals 메서드는 페이지 번호가 음수일 경우 BusinessException을 던진다.")
    fun getAnimals_fail_InvalidPageNumber() {
        // given
        //Pageable pageable = PageRequest.of(-1, 10);
        // PageRequest.of()를 쓰는 대신 Pageable을 Mock으로 만들어 객체 생성 시점의 검증을 우회
        val pageable = Mockito.mock<Pageable>(Pageable::class.java)

        // getPageNumber() 호출 시 음수(-1)를 반환하도록 설정
        BDDMockito.given(pageable.pageNumber).willReturn(-1)

        // 실행 및 검증
        Assertions.assertThatThrownBy(ThrowableAssert.ThrowingCallable {
            animalService.getAnimals(
                null,
                null,
                null,
                null,
                pageable
            )
        })
            .isInstanceOf(BusinessException::class.java)
            .satisfies({ ex: Throwable? ->
                val be = ex as BusinessException
                // 에러 코드와 메시지를 직접 꺼내서 확인
                Assertions.assertThat(be.errorCode.code).isEqualTo("A-002")
                Assertions.assertThat(be.errorCode.message).isEqualTo("잘못된 페이지 번호입니다")
            })
    }
}
