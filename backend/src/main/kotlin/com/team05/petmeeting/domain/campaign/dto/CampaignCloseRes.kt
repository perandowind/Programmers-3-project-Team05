package com.team05.petmeeting.domain.campaign.dto

import com.team05.petmeeting.domain.campaign.enums.CampaignStatus

data class CampaignCloseRes(
    val id: Long,
    val status: CampaignStatus
)
