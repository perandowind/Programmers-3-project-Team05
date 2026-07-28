package com.team05.petmeeting.domain.donation.service

import com.team05.petmeeting.domain.campaign.enums.CampaignStatus
import com.team05.petmeeting.domain.campaign.errorcode.CampaignErrorCode
import com.team05.petmeeting.domain.campaign.repository.CampaignRepository
import com.team05.petmeeting.domain.campaign.service.CampaignService
import com.team05.petmeeting.domain.donation.dto.CompleteRes
import com.team05.petmeeting.domain.donation.dto.PrepareReq
import com.team05.petmeeting.domain.donation.dto.PrepareRes
import com.team05.petmeeting.domain.donation.entity.Donation
import com.team05.petmeeting.domain.donation.enums.DonationStatus
import com.team05.petmeeting.domain.donation.errorcode.DonationErrorCode
import com.team05.petmeeting.domain.donation.repository.DonationRepository
import com.team05.petmeeting.domain.user.dto.profile.UserDonationRes
import com.team05.petmeeting.domain.user.service.UserService
import com.team05.petmeeting.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true) // Default to read-only for safety
class DonationDbService(
    private val donationRepository: DonationRepository,
    private val campaignService: CampaignService,
    private val userService: UserService,
    private val campaignRepository: CampaignRepository,
) {
    @Transactional
    fun prepareDonation(userId: Long, req: PrepareReq): PrepareRes {
        val paymentId = "donate-" + UUID.randomUUID().toString().replace("-", "")
        val user = userService.findById(userId)
        val campaign = campaignService.findById(req.campaignId)

        if (campaign.status != CampaignStatus.ACTIVE) {
            throw BusinessException(CampaignErrorCode.CAMPAIGN_CLOSED)
        }

        val donation = Donation.create(user, campaign, paymentId, req.amount)
        donationRepository.save(donation)

        return PrepareRes(paymentId, req.amount)
    }

    @Transactional
    fun completeDonation(paymentId: String, paidAmount: Int, isPaid: Boolean): CompleteRes {
        val donation = donationRepository.findByPaymentIdForUpdate(paymentId)
            ?: throw BusinessException(DonationErrorCode.PAYMENT_NOT_FOUND)

        if (donation.status == DonationStatus.PAID) {  // 웹훅 중복 처리 방지
            return donation.toCompleteRes()
        }

        if (!isPaid) {
            donation.fail()
            donationRepository.save(donation)
            throw BusinessException(DonationErrorCode.PAYMENT_NOT_PAID)
        }

        if (paidAmount != donation.amount) {
            donation.fail()
            donationRepository.save(donation)
            throw BusinessException(DonationErrorCode.AMOUNT_MISMATCH)
        }

        donation.complete()
        val campaignId = requireNotNull(donation.campaign.id)
        campaignRepository.addDonationAmount(campaignId, paidAmount)
        campaignRepository.updateStatusIfTargetReached(campaignId)

        donationRepository.save(donation)

        return donation.toCompleteRes()
    }

    @Transactional
    fun processWebhookDonation(paymentId: String, paidAmount: Int) {
        val donation = donationRepository.findByPaymentIdForUpdate(paymentId)
            ?: throw BusinessException(DonationErrorCode.DONATION_NOT_FOUND)

        if (donation.status == DonationStatus.PAID) {
            return // UI가 해결했으면 웹훅 무시
        }

        if (paidAmount != donation.amount) {
            donation.fail()
        } else {
            donation.complete()
            val campaignId = requireNotNull(donation.campaign.id)
            campaignRepository.addDonationAmount(campaignId, paidAmount)
            campaignRepository.updateStatusIfTargetReached(campaignId)
        }
        donationRepository.save(donation)
    }

    fun getMyDonations(userId: Long): UserDonationRes {
        val donations = donationRepository.findByUser_Id(userId)
        val totalAmount = donations
            .filter { it.status == DonationStatus.PAID }
            .sumOf { it.amount }
        return UserDonationRes.of(donations.size, totalAmount, donations)
    }

    private fun Donation.toCompleteRes(): CompleteRes =
        CompleteRes(
            id = requireNotNull(id),
            amount = amount,
            status = status,
            campaignId = requireNotNull(campaign.id)
        )

}
