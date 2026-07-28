package com.team05.petmeeting.domain.donation.controller

import com.team05.petmeeting.domain.donation.service.DonationDbService
import com.team05.petmeeting.domain.campaign.entity.Campaign
import com.team05.petmeeting.domain.campaign.repository.CampaignRepository
import com.team05.petmeeting.domain.donation.dto.PrepareReq
import com.team05.petmeeting.domain.donation.entity.Donation
import com.team05.petmeeting.domain.donation.enums.DonationStatus
import com.team05.petmeeting.domain.donation.errorcode.DonationErrorCode
import com.team05.petmeeting.domain.donation.repository.DonationRepository
import com.team05.petmeeting.domain.shelter.dto.ShelterCommand
import com.team05.petmeeting.domain.shelter.entity.Shelter
import com.team05.petmeeting.domain.shelter.repository.ShelterRepository
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.repository.UserRepository
import com.team05.petmeeting.global.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class DonationDbServiceTest {

    @Autowired
    lateinit var donationDbService: DonationDbService

    @Autowired
    lateinit var donationRepository: DonationRepository

    @Autowired
    lateinit var campaignRepository: CampaignRepository

    @Autowired
    lateinit var shelterRepository: ShelterRepository

    @Autowired
    lateinit var userRepository: UserRepository

    // 테스트용 임시 보호소를 생성하는 헬퍼 메서드 (중복 코드 제거)
    private fun createTestShelter(): Shelter {
        val uniqueRegNo = "REG-${UUID.randomUUID().toString().take(8)}"
        val shelterCommand = ShelterCommand(
            careRegNo = uniqueRegNo,
            careNm = "행복 유기견 보호소",
            careTel = "010-1234-5678",
            careAddr = "경기도 안산시 상록구 한양대학로 55",
            careOwnerNm = "김보호",
            orgNm = "안산동물보호협회",
            updTm = LocalDateTime.now()
        )
        return shelterRepository.save(Shelter.create(shelterCommand))
    }

    @Test
    @DisplayName("결제 준비 - PENDING 상태의 후원 내역이 DB에 생성된다")
    fun prepareDonation() {
        // given
        val user = userRepository.save(User.create(email = "test@test.com", nickname = "밍고", realname = "강민경"))
        val shelter = createTestShelter()
        val campaign = campaignRepository.save(Campaign.create(shelter, "사료 모금", "사료 지원 캠페인", 1000000))

        val reqAmount = 5000
        val req = PrepareReq(campaignId = campaign.id!!, amount = reqAmount)

        // when
        val result = donationDbService.prepareDonation(user.id!!, req)

        // then
        assertThat(result.amount).isEqualTo(reqAmount)
        assertThat(result.paymentId).startsWith("donate-")

        val savedDonation = donationRepository.findByPaymentId(result.paymentId)!!
        assertThat(savedDonation.amount).isEqualTo(reqAmount)
        assertThat(savedDonation.status).isEqualTo(DonationStatus.PENDING)
    }

    @Test
    @DisplayName("결제 완료 - 결제 성공 시 상태가 PAID로 변경되고 캠페인 금액이 증가한다")
    fun completeDonation_Success() {
        // given
        val user = userRepository.save(User.create(email = "test@test.com", nickname = "밍고", realname = "강민경"))
        val shelter = createTestShelter()
        val campaign = campaignRepository.save(Campaign.create(shelter, "난방비 후원", "난방비 지원 캠페인", 1000000))

        val paymentId = "donate-12345"
        val reqAmount = 10000

        val pendingDonation = Donation.create(user, campaign, paymentId, reqAmount)
        donationRepository.save(pendingDonation)

        // when
        val result = donationDbService.completeDonation(paymentId, reqAmount, isPaid = true)

        // then
        assertThat(result.status).isEqualTo(DonationStatus.PAID)
        assertThat(result.amount).isEqualTo(reqAmount)

        val updatedDonation = donationRepository.findByPaymentId(paymentId)!!
        assertThat(updatedDonation.status).isEqualTo(DonationStatus.PAID)

        val updatedCampaign = campaignRepository.findById(campaign.id!!).get()
        assertThat(updatedCampaign.currentAmount).isEqualTo(reqAmount)
    }

    @Test
    @DisplayName("결제 완료 - 포트원에서 결제 실패(isPaid=false) 시 상태가 FAILED로 변경된다")
    fun completeDonation_Fail_NotPaid() {
        // given
        val user = userRepository.save(User.create(email = "test@test.com", nickname = "밍고", realname = "강민경"))
        val shelter = createTestShelter()
        val campaign = campaignRepository.save(Campaign.create(shelter, "난방비 후원", "난방비 지원 캠페인", 1000000))

        val paymentId = "donate-fail"
        donationRepository.save(Donation.create(user, campaign, paymentId, 10000))

        // when & then
        val exception = assertThrows(BusinessException::class.java) {
            donationDbService.completeDonation(paymentId, 10000, isPaid = false)
        }

        assertThat(exception.errorCode).isEqualTo(DonationErrorCode.PAYMENT_NOT_PAID)

        val failedDonation = donationRepository.findByPaymentId(paymentId)!!
        assertThat(failedDonation.status).isEqualTo(DonationStatus.FAILED)
    }

    @Test
    @DisplayName("결제 완료 - 결제 요청 금액과 실제 결제 금액이 다르면 FAILED로 변경된다")
    fun completeDonation_Fail_AmountMismatch() {
        // given
        val user = userRepository.save(User.create(email = "test@test.com", nickname = "밍고", realname = "강민경"))
        val shelter = createTestShelter()
        val campaign = campaignRepository.save(Campaign.create(shelter, "난방비 후원", "난방비 지원 캠페인", 1000000))

        val paymentId = "donate-mismatch"
        donationRepository.save(Donation.create(user, campaign, paymentId, 10000))

        // when & then
        val exception = assertThrows(BusinessException::class.java) {
            donationDbService.completeDonation(paymentId, 5000, isPaid = true)
        }

        assertThat(exception.errorCode).isEqualTo(DonationErrorCode.AMOUNT_MISMATCH)

        val failedDonation = donationRepository.findByPaymentId(paymentId)!!
        assertThat(failedDonation.status).isEqualTo(DonationStatus.FAILED)
    }

    @Test
    @DisplayName("내 후원 내역 - 결제 완료(PAID)된 내역만 합산하여 반환한다")
    fun getMyDonations() {
        // given
        val user = userRepository.save(User.create(email = "test@test.com", nickname = "밍고", realname = "강민경"))
        val shelter = createTestShelter()
        val campaign = campaignRepository.save(Campaign.create(shelter, "테스트 모금", "설명", 100000))

        val paid1 = Donation.create(user, campaign, "donate-1", 5000).apply { complete() }
        val paid2 = Donation.create(user, campaign, "donate-2", 10000).apply { complete() }
        val pending = Donation.create(user, campaign, "donate-3", 7000)

        donationRepository.saveAll(listOf(paid1, paid2, pending))

        // when
        val result = donationDbService.getMyDonations(user.id!!)

        // then
        assertThat(result.donationCount).isEqualTo(3)
        assertThat(result.donationTotalAmount).isEqualTo(15000)
    }
}
