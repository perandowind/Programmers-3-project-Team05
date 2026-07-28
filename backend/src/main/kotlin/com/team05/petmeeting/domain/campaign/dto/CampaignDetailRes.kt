package com.team05.petmeeting.domain.campaign.dto

import com.team05.petmeeting.domain.campaign.entity.Campaign
import com.team05.petmeeting.domain.campaign.enums.CampaignStatus

data class CampaignDetailRes(
    val campaignCount: Int,
    val campaigns: List<CampaignDetailItem>
) {
    data class CampaignDetailItem(
        val id: Long,
        val title: String,
        val targetAmount: Int,
        val currentAmount: Int,
        val status: CampaignStatus
    ) {
        companion object {
            fun from(campaign: Campaign): CampaignDetailItem {
                return CampaignDetailItem(
                    requireNotNull(campaign.id),
                    campaign.title,
                    campaign.targetAmount,
                    campaign.currentAmount,
                    campaign.status
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun from(campaign: List<Campaign>): CampaignDetailRes {
            val campaignDetailItems = campaign.map(CampaignDetailItem::from)
            return CampaignDetailRes(campaignDetailItems.size, campaignDetailItems)
        }
    }
}
