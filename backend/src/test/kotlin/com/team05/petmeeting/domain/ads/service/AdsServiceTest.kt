package com.team05.petmeeting.domain.ads.service

import com.team05.petmeeting.domain.ads.dto.CardNewsResult
import com.team05.petmeeting.domain.ads.entity.AdsPostRequest
import com.team05.petmeeting.domain.ads.entity.AdsPostStatus
import com.team05.petmeeting.domain.ads.repository.AdsPostRequestRepository
import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.animal.repository.AnimalRepository
import com.team05.petmeeting.domain.shelter.dto.ShelterCommand
import com.team05.petmeeting.domain.shelter.entity.Shelter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Pageable
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
internal class AdsServiceTest {
    @Mock
    private lateinit var animalRepository: AnimalRepository

    @Mock
    private lateinit var cardNewsService: CardNewsService

    @Mock
    private lateinit var adsPostRequestRepository: AdsPostRequestRepository

    private lateinit var adsService: AdsService

    @BeforeEach
    fun setUp() {
        adsService = AdsService(
            animalRepository,
            cardNewsService,
            adsPostRequestRepository,
            transactionTemplate(),
        )
    }

    @Test
    @DisplayName("Top N 동물 조회 테스트")
    fun getTopAnimals() {
        val animal = Animal()
        Mockito.`when`(
            animalRepository.findAllByStateGroupOrderByTotalCheerCountDesc(
                eq(0),
                anyPageable()
            )
        ).thenReturn(listOf(animal))

        val result = adsService.getTopAnimals(3)

        assertThat(result).hasSize(1)
        assertThat(result[0]).isSameAs(animal)
    }

    @Test
    @DisplayName("카드뉴스 미리보기는 인스타그램 업로드 없이 승인 대기 요청으로 저장한다")
    fun createCardNewsPostRequestsDoesNotUploadToInstagram() {
        val shelter = createShelter()
        val animal = Animal.builder()
            .desertionNo("A-001")
            .processState("보호중")
            .stateGroup(0)
            .kindFullNm("[개] 믹스견")
            .age("2026(년생)")
            .sexCd("M")
            .careNm("음성군 동물보호센터")
            .specialMark("활발함")
            .popfile1("https://image-url.com/animal.png")
            .shelter(shelter)
            .build()
        ReflectionTestUtils.setField(animal, "id", 1L)

        Mockito.`when`(
            animalRepository.findAllByStateGroupOrderByTotalCheerCountDesc(
                eq(0),
                anyPageable()
            )
        ).thenReturn(listOf(animal))
        Mockito.`when`(cardNewsService.generateCardNews(animal))
            .thenReturn(CardNewsResult("https://image-url.com/card.png", "caption"))
        Mockito.`when`(
            adsPostRequestRepository.save(anyAdsPostRequest())
        ).thenAnswer {
            val request = it.arguments[0] as AdsPostRequest
            ReflectionTestUtils.setField(request, "id", 10L)
            request
        }

        val result = adsService.createCardNewsPostRequests(1)

        assertThat(result).hasSize(1)
        assertThat(result[0].requestId).isEqualTo(10L)
        assertThat(result[0].status).isEqualTo(AdsPostStatus.Processing)
        assertThat(result[0].imageUrl).isEqualTo("https://image-url.com/card.png")
        assertThat(result[0].caption).isEqualTo("caption")
        Mockito.verify(adsPostRequestRepository).save(anyAdsPostRequest())
    }

    private fun anyPageable(): Pageable {
        return any(Pageable::class.java) ?: Pageable.unpaged()
    }

    private fun anyAdsPostRequest(): AdsPostRequest {
        return any(AdsPostRequest::class.java)
            ?: AdsPostRequest.create(Animal(), createShelter(), "image", "caption")
    }

    private fun transactionTemplate(): TransactionTemplate =
        TransactionTemplate(
            object : PlatformTransactionManager {
                override fun getTransaction(definition: TransactionDefinition?): TransactionStatus =
                    SimpleTransactionStatus()

                override fun commit(status: TransactionStatus) = Unit

                override fun rollback(status: TransactionStatus) = Unit
            },
        )

    private fun createShelter(): Shelter {
        return Shelter.create(
            ShelterCommand(
                "343447202600001",
                "음성군 동물보호센터",
                "043-000-0000",
                "충청북도 음성군",
                "음성군수",
                "충청북도 음성군",
                LocalDateTime.now(),
            ),
        )
    }
}
