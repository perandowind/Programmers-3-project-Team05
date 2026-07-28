package com.team05.petmeeting.domain.ads.service

import com.team05.petmeeting.domain.ads.dto.AdsPostRequestRes
import com.team05.petmeeting.domain.ads.entity.AdsPostRequest
import com.team05.petmeeting.domain.ads.errorCode.AdsErrorCode
import com.team05.petmeeting.domain.ads.repository.AdsPostRequestRepository
import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.animal.repository.AnimalRepository
import com.team05.petmeeting.domain.shelter.entity.Shelter
import com.team05.petmeeting.global.exception.BusinessException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Service
class AdsService(
    private val animalRepository: AnimalRepository,
    private val cardNewsService: CardNewsService,
    private val adsPostRequestRepository: AdsPostRequestRepository,
    private val transactionTemplate: TransactionTemplate,
) {
    // Top N 동물 조회 (보호중인 동물만)
    @Transactional(readOnly = true)
    fun getTopAnimals(n: Int): List<Animal> {
        return animalRepository.findAllByStateGroupOrderByTotalCheerCountDesc(
            0,  // 0 = 보호중
            PageRequest.of(0, n)
        )
    }

    // 인스타그램 업로드 없이 카드뉴스를 생성하고 승인 대기 요청으로 저장
    fun createCardNewsPostRequests(n: Int): List<AdsPostRequestRes> {
        val targets = transactionTemplate.execute {
            animalRepository.findAllByStateGroupOrderByTotalCheerCountDesc(
                0,
                PageRequest.of(0, n),
            ).map { animal ->
                val shelter = animal.shelter
                    ?: throw BusinessException(AdsErrorCode.ANIMAL_SHELTER_NOT_FOUND)

                CardNewsTarget(animal, shelter)
            }
        }.orEmpty()

        val generatedRequests = targets.map { target ->
            val cardNews = cardNewsService.generateCardNews(target.animal)
            GeneratedCardNews(
                animal = target.animal,
                shelter = target.shelter,
                imageUrl = requireNotNull(cardNews.imageUrl),
                caption = requireNotNull(cardNews.caption),
            )
        }

        return transactionTemplate.execute {
            generatedRequests.map { generated ->
                adsPostRequestRepository.save(
                    AdsPostRequest.create(
                        animal = generated.animal,
                        shelter = generated.shelter,
                        imageUrl = generated.imageUrl,
                        caption = generated.caption,
                    ),
                )
            }.map { AdsPostRequestRes.from(it) }
        }.orEmpty()
    }

    // @Scheduled(cron = "0 0 9 * * MON") // 매주 월요일 오전 9시 승인 대기 요청 생성
    fun scheduledWeeklyAds() {
        createCardNewsPostRequests(DEFAULT_WEEKLY_ADS_COUNT)
    }

    companion object {
        private const val DEFAULT_WEEKLY_ADS_COUNT = 3
    }

    private data class CardNewsTarget(
        val animal: Animal,
        val shelter: Shelter,
    )

    private data class GeneratedCardNews(
        val animal: Animal,
        val shelter: Shelter,
        val imageUrl: String,
        val caption: String,
    )
}
