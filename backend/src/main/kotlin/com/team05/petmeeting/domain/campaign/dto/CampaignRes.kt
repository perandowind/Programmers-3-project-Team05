package com.team05.petmeeting.domain.campaign.dto

import com.team05.petmeeting.domain.campaign.entity.Campaign
import com.team05.petmeeting.domain.campaign.enums.CampaignStatus

data class CampaignRes(
    val totalCampaigns: Int,
    val campaigns: List<CampaignItem>
) {
    data class CampaignItem(
        val id: Long,
        val title: String,
        val description: String,
        val targetAmount: Int,
        val currentAmount: Int,
        val status: CampaignStatus,
        val shelterId: String
    ) {
        companion object {
            fun from(campaign: Campaign): CampaignItem {
                return CampaignItem(
                    requireNotNull(campaign.id),
                    campaign.title,
                    campaign.description,
                    campaign.targetAmount,
                    campaign.currentAmount,
                    campaign.status,
                    campaign.shelter.careRegNo
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun of(totalCampaigns: Int, campaignList: List<Campaign>): CampaignRes {
            val items = campaignList.map(CampaignItem::from)
            return CampaignRes(totalCampaigns, items)
        }
    }
}
