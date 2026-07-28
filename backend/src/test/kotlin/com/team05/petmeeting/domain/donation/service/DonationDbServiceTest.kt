package com.team05.petmeeting.domain.donation.service
import com.team05.petmeeting.domain.campaign.entity.Campaign
import com.team05.petmeeting.domain.campaign.enums.CampaignStatus
import com.team05.petmeeting.domain.campaign.errorcode.CampaignErrorCode
import com.team05.petmeeting.domain.campaign.repository.CampaignRepository
import com.team05.petmeeting.domain.campaign.service.CampaignService
import com.team05.petmeeting.domain.donation.dto.PrepareReq
import com.team05.petmeeting.domain.donation.entity.Donation
import com.team05.petmeeting.domain.donation.enums.DonationStatus
import com.team05.petmeeting.domain.donation.errorcode.DonationErrorCode
import com.team05.petmeeting.domain.donation.repository.DonationRepository
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.service.UserService
import com.team05.petmeeting.global.entity.BaseEntity
import com.team05.petmeeting.global.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
class DonationDbServiceTest {

    @Mock
    lateinit var donationRepository: DonationRepository

    @Mock
    lateinit var campaignService: CampaignService

    @Mock
    lateinit var userService: UserService

    @Mock
    lateinit var campaignRepository: CampaignRepository

    @InjectMocks
    lateinit var donationDbService: DonationDbService

    private fun setId(entity: Any, id: Long) {
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }

    private fun mockCampaign(status: CampaignStatus = CampaignStatus.ACTIVE): Campaign {
        val campaign = mock<Campaign>()
        lenient().`when`(campaign.id).thenReturn(11L)
        lenient().`when`(campaign.status).thenReturn(status)
        return campaign
    }

    private fun mockUser(): User {
        val user = mock<User>()
        lenient().`when`(user.id).thenReturn(1L)
        return user
    }

    private fun mockDonation(amount: Int = 10000): Donation {
        val donation = Donation.create(
            user = mockUser(),
            campaign = mockCampaign(),
            paymentId = "donate-test123",
            amount = amount
        )
        setId(donation, 1L)
        return donation
    }

    // ===== prepareDonation =====

    @Test
    @DisplayName("결제 준비 성공")
    fun prepareDonation_success() {
        val user = mockUser()
        val campaign = mockCampaign()
        val req = PrepareReq(11L, 10000)

        whenever(userService.findById(1L)).thenReturn(user)
        whenever(campaignService.findById(11L)).thenReturn(campaign)
        whenever(donationRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = donationDbService.prepareDonation(1L, req)

        assertThat(result.amount).isEqualTo(10000)
        assertThat(result.paymentId).startsWith("donate-")
        verify(donationRepository).save(any())
    }

    @Test
    @DisplayName("결제 준비 실패 - 마감된 캠페인")
    fun prepareDonation_fail_closedCampaign() {
        val user = mockUser()
        val campaign = mockCampaign(CampaignStatus.CLOSED)
        val req = PrepareReq(11L, 10000)

        whenever(userService.findById(1L)).thenReturn(user)
        whenever(campaignService.findById(11L)).thenReturn(campaign)

        assertThrows<BusinessException> {
            donationDbService.prepareDonation(1L, req)
        }.also {
            assertThat(it.errorCode).isEqualTo(CampaignErrorCode.CAMPAIGN_CLOSED)
        }
    }

    // ===== completeDonation =====

    @Test
    @DisplayName("결제 완료 성공")
    fun completeDonation_success() {
        val donation = mockDonation(10000)
        whenever(donationRepository.findByPaymentIdForUpdate("donate-123")).thenReturn(donation)

        val result = donationDbService.completeDonation("donate-123", 10000, true)

        assertThat(donation.status).isEqualTo(DonationStatus.PAID)  // verify 대신 상태 확인
        verify(campaignRepository).addDonationAmount(11L, 10000)
        verify(campaignRepository).updateStatusIfTargetReached(11L)
        assertThat(result.amount).isEqualTo(10000)
        assertThat(result.campaignId).isEqualTo(11L)
    }

    @Test
    @DisplayName("결제 완료 실패 - 결제 안 됨")
    fun completeDonation_fail_notPaid() {
        val donation = mockDonation()
        whenever(donationRepository.findByPaymentIdForUpdate("donate-123")).thenReturn(donation)

        assertThrows<BusinessException> {
            donationDbService.completeDonation("donate-123", 10000, false)
        }.also {
            assertThat(it.errorCode).isEqualTo(DonationErrorCode.PAYMENT_NOT_PAID)
        }

        assertThat(donation.status).isEqualTo(DonationStatus.FAILED)  // verify 대신
    }

    @Test
    @DisplayName("결제 완료 실패 - 금액 불일치")
    fun completeDonation_fail_amountMismatch() {
        val donation = mockDonation(10000)
        whenever(donationRepository.findByPaymentIdForUpdate("donate-123")).thenReturn(donation)

        assertThrows<BusinessException> {
            donationDbService.completeDonation("donate-123", 5000, true)
        }.also {
            assertThat(it.errorCode).isEqualTo(DonationErrorCode.AMOUNT_MISMATCH)
        }

        assertThat(donation.status).isEqualTo(DonationStatus.FAILED)  // verify 대신
    }

    @Test
    @DisplayName("결제 완료 실패 - 후원 내역 없음")
    fun completeDonation_fail_donationNotFound() {
        whenever(donationRepository.findByPaymentIdForUpdate("donate-999")).thenReturn(null)

        assertThrows<BusinessException> {
            donationDbService.completeDonation("donate-999", 10000, true)
        }.also {
            assertThat(it.errorCode).isEqualTo(DonationErrorCode.PAYMENT_NOT_FOUND)
        }
    }

    // ===== processWebhookDonation =====

    @Test
    @DisplayName("웹훅 처리 성공")
    fun processWebhookDonation_success() {
        val donation = mockDonation(10000)
        whenever(donationRepository.findByPaymentIdForUpdate("donate-123")).thenReturn(donation)

        donationDbService.processWebhookDonation("donate-123", 10000)

        assertThat(donation.status).isEqualTo(DonationStatus.PAID)  // verify 대신
        verify(campaignRepository).addDonationAmount(11L, 10000)
        verify(campaignRepository).updateStatusIfTargetReached(11L)
    }

    @Test
    @DisplayName("웹훅 처리 - 금액 불일치시 fail 처리")
    fun processWebhookDonation_amountMismatch() {
        val donation = mockDonation(10000)
        whenever(donationRepository.findByPaymentIdForUpdate("donate-123")).thenReturn(donation)

        donationDbService.processWebhookDonation("donate-123", 5000)

        assertThat(donation.status).isEqualTo(DonationStatus.FAILED)  // verify 대신
        verify(campaignRepository, never()).addDonationAmount(any(), any())
    }

    @Test
    @DisplayName("웹훅 처리 실패 - 후원 내역 없음")
    fun processWebhookDonation_fail_donationNotFound() {
        whenever(donationRepository.findByPaymentIdForUpdate("donate-999")).thenReturn(null)

        assertThrows<BusinessException> {
            donationDbService.processWebhookDonation("donate-999", 10000)
        }.also {
            assertThat(it.errorCode).isEqualTo(DonationErrorCode.DONATION_NOT_FOUND)
        }
    }

    // ===== getMyDonations =====

    @Test
    @DisplayName("내 후원 내역 조회 성공")
    fun getMyDonations_success() {
        val paidDonation = mock<Donation>().also {
            setId(it, 1L)
            val campaign = mockCampaign()  // id가 11L로 설정됨
            whenever(it.campaign).thenReturn(campaign)
            whenever(it.status).thenReturn(DonationStatus.PAID)
            whenever(it.amount).thenReturn(10000)
        }
        val pendingDonation = mock<Donation>().also {
            setId(it, 2L)
            val campaign = mockCampaign()
            whenever(it.campaign).thenReturn(campaign)
            whenever(it.status).thenReturn(DonationStatus.PENDING)
            whenever(it.amount).thenReturn(5000)
        }

        whenever(donationRepository.findByUser_Id(1L))
            .thenReturn(listOf(paidDonation, pendingDonation))

        val result = donationDbService.getMyDonations(1L)

        assertThat(result.donationCount).isEqualTo(2)
        assertThat(result.donationTotalAmount).isEqualTo(10000)  // PAID만 합산
    }
}
