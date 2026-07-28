package com.team05.petmeeting.domain.ads.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.team05.petmeeting.domain.ads.client.InstagramClient
import com.team05.petmeeting.domain.ads.dto.AdsPostReviewReq
import com.team05.petmeeting.domain.ads.dto.CardNewsResult
import com.team05.petmeeting.domain.ads.entity.AdsPostRequest
import com.team05.petmeeting.domain.ads.entity.AdsPostStatus
import com.team05.petmeeting.domain.ads.errorCode.AdsErrorCode
import com.team05.petmeeting.domain.ads.repository.AdsPostRequestRepository
import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.shelter.dto.ShelterCommand
import com.team05.petmeeting.domain.shelter.entity.Shelter
import com.team05.petmeeting.domain.shelter.repository.ShelterRepository
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.global.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
internal class AdsAdminServiceTest {

    @Mock
    private lateinit var adsPostRequestRepository: AdsPostRequestRepository

    @Mock
    private lateinit var shelterRepository: ShelterRepository

    @Mock
    private lateinit var instagramClient: InstagramClient

    @Mock
    private lateinit var cardNewsService: CardNewsService

    private lateinit var adsAdminService: AdsAdminService

    @BeforeEach
    fun setUp() {
        adsAdminService = AdsAdminService(
            adsPostRequestRepository,
            shelterRepository,
            instagramClient,
            cardNewsService,
            ObjectMapper(),
            transactionTemplate(),
        )
    }

    @Test
    @DisplayName("보호소 관리자가 광고 게시 요청을 승인하면 인스타그램에 업로드한다")
    fun reviewRequestApprovePublishesInstagram() {
        val manager = createUser(1L)
        val shelter = createShelter(manager)
        val request = createRequest(10L, shelter)

        Mockito.`when`(shelterRepository.findById("343447202600001"))
            .thenReturn(Optional.of(shelter))
        Mockito.`when`(adsPostRequestRepository.findById(10L))
            .thenReturn(Optional.of(request))
        Mockito.`when`(
            instagramClient.createMediaContainer(
                "https://image-url.com/card.png",
                "caption",
            ),
        ).thenReturn("{ \"id\" : \"container-123\" }")
        Mockito.`when`(instagramClient.getContainerStatus("container-123"))
            .thenReturn("{ \"status_code\" : \"FINISHED\" }")

        val response = adsAdminService.reviewRequest(
            userId = 1L,
            careRegNo = "343447202600001",
            requestId = 10L,
            reviewReq = AdsPostReviewReq(AdsPostStatus.Approved, null),
        )

        assertThat(response.status).isEqualTo(AdsPostStatus.Published)
        assertThat(request.reviewedAt).isNotNull()
        assertThat(request.publishedAt).isNotNull()
        Mockito.verify(instagramClient).publishMedia("container-123")
    }

    @Test
    @DisplayName("보호소 관리자가 광고 게시 요청을 거절하면 사유 없이 업로드하지 않는다")
    fun reviewRequestRejectWithoutReasonDoesNotPublishInstagram() {
        val manager = createUser(1L)
        val shelter = createShelter(manager)
        val request = createRequest(10L, shelter)

        Mockito.`when`(shelterRepository.findById("343447202600001"))
            .thenReturn(Optional.of(shelter))
        Mockito.`when`(adsPostRequestRepository.findById(10L))
            .thenReturn(Optional.of(request))

        val response = adsAdminService.reviewRequest(
            userId = 1L,
            careRegNo = "343447202600001",
            requestId = 10L,
            reviewReq = AdsPostReviewReq(AdsPostStatus.Rejected, null),
        )

        assertThat(response.status).isEqualTo(AdsPostStatus.Rejected)
        assertThat(response.rejectionReason).isNull()
        Mockito.verifyNoInteractions(instagramClient)
    }

    @Test
    @DisplayName("거절된 광고 게시 요청을 재생성하고 다시 검토중으로 변경한다")
    fun reviewRequestMarkProcessing() {
        val manager = createUser(1L)
        val shelter = createShelter(manager)
        val request = createRequest(10L, shelter)
        request.reject("이전 거절 사유")

        Mockito.`when`(shelterRepository.findById("343447202600001"))
            .thenReturn(Optional.of(shelter))
        Mockito.`when`(adsPostRequestRepository.findById(10L))
            .thenReturn(Optional.of(request))
        Mockito.`when`(cardNewsService.generateCardNews(request.animal))
            .thenReturn(CardNewsResult("https://image-url.com/regenerated-card.png", "regenerated caption"))

        val response = adsAdminService.reviewRequest(
            userId = 1L,
            careRegNo = "343447202600001",
            requestId = 10L,
            reviewReq = AdsPostReviewReq(AdsPostStatus.Processing, null),
        )

        assertThat(response.status).isEqualTo(AdsPostStatus.Processing)
        assertThat(response.rejectionReason).isNull()
        assertThat(response.imageUrl).isEqualTo("https://image-url.com/regenerated-card.png")
        assertThat(response.caption).isEqualTo("regenerated caption")
        assertThat(request.reviewedAt).isNull()
        Mockito.verifyNoInteractions(instagramClient)
    }

    @Test
    @DisplayName("해당 보호소 관리자가 아니면 광고 게시 요청을 처리할 수 없다")
    fun reviewRequestUnauthorizedShelter() {
        val otherManager = createUser(2L)
        val shelter = createShelter(otherManager)

        Mockito.`when`(shelterRepository.findById("343447202600001"))
            .thenReturn(Optional.of(shelter))

        assertThatThrownBy {
            adsAdminService.reviewRequest(
                userId = 1L,
                careRegNo = "343447202600001",
                requestId = 10L,
                reviewReq = AdsPostReviewReq(AdsPostStatus.Approved, null),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(AdsErrorCode.UNAUTHORIZED_SHELTER)
    }

    private fun createRequest(id: Long, shelter: Shelter): AdsPostRequest {
        val animal = Animal.builder()
            .desertionNo("A-001")
            .processState("보호중")
            .stateGroup(0)
            .kindFullNm("[개] 믹스견")
            .age("2026(년생)")
            .sexCd("M")
            .careNm(shelter.careNm)
            .specialMark("활발함")
            .popfile1("https://image-url.com/animal.png")
            .shelter(shelter)
            .build()
        ReflectionTestUtils.setField(animal, "id", 1L)

        val request = AdsPostRequest.create(
            animal = animal,
            shelter = shelter,
            imageUrl = "https://image-url.com/card.png",
            caption = "caption",
        )
        ReflectionTestUtils.setField(request, "id", id)
        return request
    }

    private fun createShelter(manager: User): Shelter {
        val shelter = Shelter.create(
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
        shelter.assignUser(manager)
        return shelter
    }

    private fun createUser(id: Long): User {
        val user = User.create("manager$id@test.com", "manager$id", "관리자$id")
        ReflectionTestUtils.setField(user, "id", id)
        return user
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
}
