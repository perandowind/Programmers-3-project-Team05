package com.team05.petmeeting.domain.donation.dto

import com.team05.petmeeting.domain.donation.enums.DonationStatus

data class CompleteRes(
    val id: Long,
    val amount: Int,
    val status: DonationStatus,
    val campaignId: Long
) {
    companion object {
        @JvmStatic
        fun create(): CompleteRes {
            return CompleteRes(1L, 10000, DonationStatus.PENDING, 1L)
        }
    }
}
