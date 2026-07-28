package com.team05.petmeeting.domain.campaign.service

import com.team05.petmeeting.domain.campaign.dto.CampaignCreateReq
import com.team05.petmeeting.domain.campaign.dto.CampaignCreateRes
import com.team05.petmeeting.domain.campaign.dto.CampaignCreateRes.Companion.from
import com.team05.petmeeting.domain.campaign.dto.CampaignDetailRes
import com.team05.petmeeting.domain.campaign.dto.CampaignRes
import com.team05.petmeeting.domain.campaign.dto.CampaignRes.Companion.of
import com.team05.petmeeting.domain.campaign.entity.Campaign
import com.team05.petmeeting.domain.campaign.enums.CampaignStatus
import com.team05.petmeeting.domain.campaign.errorcode.CampaignErrorCode
import com.team05.petmeeting.domain.campaign.repository.CampaignRepository
import com.team05.petmeeting.domain.shelter.service.ShelterService
import com.team05.petmeeting.global.exception.BusinessException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
@Transactional
class CampaignService(private val campaignRepository: CampaignRepository, private val shelterService: ShelterService) {
    fun createCampaign(shelterId: String, userId: Long, req: CampaignCreateReq): CampaignCreateRes {
        val shelter = shelterService.findById(shelterId)
        if (!shelter.isManagedBy(userId)) {
            throw BusinessException(CampaignErrorCode.UNAUTHORIZED_SHELTER)
        }

        if (campaignRepository.existsByShelter_CareRegNoAndStatus(shelterId, CampaignStatus.ACTIVE)) {
            throw BusinessException(CampaignErrorCode.CAMPAIGN_ALREADY_EXISTS)
        }

        val campaign = Campaign.create(shelter, req.title, req.description, req.amount)
        campaignRepository.save(campaign)
        return from(campaign)
    }

    fun getCampaign(shelterId: String): CampaignDetailRes {
        val shelter = shelterService.findById(shelterId)
        val campaignList = campaignRepository.findByShelterOrderByStatusNative(shelter)
        return CampaignDetailRes.from(campaignList)
    }

    fun closeCampaign(userId: Long, id: Long) {
        val campaign = getCampaignOrThrow(userId, id)
        if (campaign.status != CampaignStatus.ACTIVE) {
            throw BusinessException(CampaignErrorCode.CAMPAIGN_CLOSED)
        }
        campaign.close()
    }

    private fun getCampaignOrThrow(userId: Long, campaignId: Long): Campaign {
        val campaign = campaignRepository.findById(campaignId)
            .orElseThrow { BusinessException(CampaignErrorCode.CAMPAIGN_NOT_FOUND) }
        if (!campaign.shelter.isManagedBy(userId)) {
            throw BusinessException(CampaignErrorCode.UNAUTHORIZED_SHELTER)
        }
        return campaign
    }

    val allCampaigns: CampaignRes
        get() {
            val campaigns = campaignRepository.findAll()
            return of(campaigns.size, campaigns)
        }

    fun findById(campaignId: Long): Campaign {
        return campaignRepository.findById(campaignId).orElseThrow{ BusinessException(CampaignErrorCode.CAMPAIGN_NOT_FOUND) }
    }
}
