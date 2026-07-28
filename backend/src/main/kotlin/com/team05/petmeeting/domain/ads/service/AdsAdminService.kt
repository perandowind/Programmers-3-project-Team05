package com.team05.petmeeting.domain.ads.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.team05.petmeeting.domain.ads.client.InstagramClient
import com.team05.petmeeting.domain.ads.dto.AdsPostRequestRes
import com.team05.petmeeting.domain.ads.dto.AdsPostReviewReq
import com.team05.petmeeting.domain.ads.entity.AdsPostRequest
import com.team05.petmeeting.domain.ads.entity.AdsPostStatus
import com.team05.petmeeting.domain.ads.errorCode.AdsErrorCode
import com.team05.petmeeting.domain.ads.repository.AdsPostRequestRepository
import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.shelter.entity.Shelter
import com.team05.petmeeting.domain.shelter.errorcode.ShelterErrorCode
import com.team05.petmeeting.domain.shelter.repository.ShelterRepository
import com.team05.petmeeting.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Service
class AdsAdminService(
    private val adsPostRequestRepository: AdsPostRequestRepository,
    private val shelterRepository: ShelterRepository,
    private val instagramClient: InstagramClient,
    private val cardNewsService: CardNewsService,
    private val objectMapper: ObjectMapper,
    private val transactionTemplate: TransactionTemplate,
) {
    @Transactional(readOnly = true)
    fun getManagedShelterRequests(userId: Long, careRegNo: String): List<AdsPostRequestRes> {
        validateShelterManager(userId, careRegNo)

        return adsPostRequestRepository.findByShelter_CareRegNo(careRegNo)
            .map { AdsPostRequestRes.from(it) }
    }

    @Transactional(readOnly = true)
    fun getManagedShelterRequestDetail(
        userId: Long,
        careRegNo: String,
        requestId: Long,
    ): AdsPostRequestRes {
        validateShelterManager(userId, careRegNo)

        val request = getShelterRequest(careRegNo, requestId)
        return AdsPostRequestRes.from(request)
    }

    fun reviewRequest(
        userId: Long,
        careRegNo: String,
        requestId: Long,
        reviewReq: AdsPostReviewReq,
    ): AdsPostRequestRes {
        return when (reviewReq.status) {
            AdsPostStatus.Approved -> approveAndPublish(userId, careRegNo, requestId)
            AdsPostStatus.Rejected -> rejectRequest(userId, careRegNo, requestId, reviewReq.rejectionReason)
            AdsPostStatus.Processing -> requestReviewAgain(userId, careRegNo, requestId)
            AdsPostStatus.Published -> throw BusinessException(AdsErrorCode.INVALID_REVIEW_STATUS)
        }
    }

    private fun approveAndPublish(userId: Long, careRegNo: String, requestId: Long): AdsPostRequestRes {
        val publishTarget = transactionTemplate.execute {
            validateShelterManager(userId, careRegNo)
            val request = getShelterRequest(careRegNo, requestId)
            if (request.status != AdsPostStatus.Processing) {
                throw BusinessException(AdsErrorCode.ALREADY_REVIEWED)
            }

            request.approve()
            InstagramPublishTarget(request.imageUrl, request.caption)
        } ?: throw BusinessException(AdsErrorCode.POST_REQUEST_NOT_FOUND)

        publishToInstagram(publishTarget)

        return transactionTemplate.execute {
            val request = getShelterRequest(careRegNo, requestId)
            request.markPublished()
            AdsPostRequestRes.from(request)
        } ?: throw BusinessException(AdsErrorCode.POST_REQUEST_NOT_FOUND)
    }

    private fun rejectRequest(
        userId: Long,
        careRegNo: String,
        requestId: Long,
        rejectionReason: String?,
    ): AdsPostRequestRes =
        transactionTemplate.execute {
            validateShelterManager(userId, careRegNo)
            val request = getShelterRequest(careRegNo, requestId)
            if (request.status != AdsPostStatus.Processing) {
                throw BusinessException(AdsErrorCode.ALREADY_REVIEWED)
            }

            request.reject(rejectionReason)
            AdsPostRequestRes.from(request)
        } ?: throw BusinessException(AdsErrorCode.POST_REQUEST_NOT_FOUND)

    private fun requestReviewAgain(userId: Long, careRegNo: String, requestId: Long): AdsPostRequestRes {
        val target = transactionTemplate.execute {
            validateShelterManager(userId, careRegNo)
            val request = getShelterRequest(careRegNo, requestId)
            if (request.status != AdsPostStatus.Rejected) {
                throw BusinessException(AdsErrorCode.ALREADY_REVIEWED)
            }

            request.animal.toCardNewsTarget()
        } ?: throw BusinessException(AdsErrorCode.POST_REQUEST_NOT_FOUND)

        val cardNews = cardNewsService.generateCardNews(target.animal)

        return transactionTemplate.execute {
            val request = getShelterRequest(careRegNo, requestId)
            request.replaceCardNews(
                imageUrl = requireNotNull(cardNews.imageUrl),
                caption = requireNotNull(cardNews.caption),
            )
            request.markProcessing()
            AdsPostRequestRes.from(request)
        } ?: throw BusinessException(AdsErrorCode.POST_REQUEST_NOT_FOUND)
    }

    private fun publishToInstagram(target: InstagramPublishTarget) {
        val containerResponse = instagramClient.createMediaContainer(
            target.imageUrl,
            target.caption,
        ) ?: throw IllegalStateException("인스타그램 미디어 컨테이너 응답이 비어있습니다.")
        val containerId = extractId(containerResponse)

        waitUntilContainerReady(containerId)
        instagramClient.publishMedia(containerId)
    }

    private fun getShelterRequest(careRegNo: String, requestId: Long): AdsPostRequest {
        val request = adsPostRequestRepository.findById(requestId)
            .orElseThrow { BusinessException(AdsErrorCode.POST_REQUEST_NOT_FOUND) }

        if (!isShelterRequest(request, careRegNo)) {
            throw BusinessException(AdsErrorCode.FORBIDDEN_SHELTER_REQUEST)
        }

        return request
    }

    private fun validateShelterManager(userId: Long, careRegNo: String) {
        val shelter = shelterRepository.findById(careRegNo)
            .orElseThrow { BusinessException(ShelterErrorCode.SHELTER_NOT_FOUND) }

        if (!shelter.isManagedBy(userId)) {
            throw BusinessException(AdsErrorCode.UNAUTHORIZED_SHELTER)
        }
    }

    private fun isShelterRequest(request: AdsPostRequest, careRegNo: String): Boolean {
        val shelter: Shelter = request.shelter
        return shelter.careRegNo == careRegNo
    }

    private fun extractId(response: String): String {
        return try {
            objectMapper.readTree(response)
                .path("id")
                .asText(null)
                ?: throw IllegalStateException("인스타그램 응답에서 id를 찾을 수 없습니다.")
        } catch (e: Exception) {
            throw IllegalStateException("인스타그램 미디어 컨테이너 ID 추출 실패", e)
        }
    }

    private fun waitUntilContainerReady(containerId: String) {
        repeat(MAX_STATUS_CHECK_COUNT) {
            val statusResponse = instagramClient.getContainerStatus(containerId)
            val statusCode = extractStatusCode(statusResponse)

            if (statusCode == "FINISHED") {
                return
            }

            if (statusCode == "ERROR") {
                throw IllegalStateException("인스타그램 미디어 컨테이너 처리 실패")
            }

            Thread.sleep(STATUS_CHECK_INTERVAL_MILLIS)
        }

        throw IllegalStateException("인스타그램 미디어 컨테이너 처리 시간 초과")
    }

    private fun extractStatusCode(response: String): String {
        return try {
            objectMapper.readTree(response)
                .path("status_code")
                .asText(null)
                ?: throw IllegalStateException("인스타그램 응답에서 status_code를 찾을 수 없습니다.")
        } catch (e: Exception) {
            throw IllegalStateException("인스타그램 미디어 컨테이너 상태 추출 실패", e)
        }
    }

    companion object {
        private const val MAX_STATUS_CHECK_COUNT = 5
        private const val STATUS_CHECK_INTERVAL_MILLIS = 3000L
    }

    private data class InstagramPublishTarget(
        val imageUrl: String,
        val caption: String,
    )

    private data class CardNewsTarget(
        val animal: Animal,
    )

    private fun Animal.toCardNewsTarget(): CardNewsTarget {
        desertionNo
        kindFullNm
        age
        sexCd
        careNm
        specialMark
        popfile1
        return CardNewsTarget(this)
    }
}
