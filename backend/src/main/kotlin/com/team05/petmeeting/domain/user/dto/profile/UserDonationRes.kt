package com.team05.petmeeting.domain.user.dto.profile

import com.team05.petmeeting.domain.donation.entity.Donation
import com.team05.petmeeting.domain.donation.enums.DonationStatus

data class UserDonationRes(
    val donationCount: Int,
    val donationTotalAmount: Int,
    val donations: List<UserDonationItem>
) {

    data class UserDonationItem(
        val id: Long,
        val amount: Int,
        val status: DonationStatus,
        val campaignId: Long
    ) {
        companion object {
            @JvmStatic
            fun from(donation: Donation): UserDonationItem {
                return UserDonationItem(
                    requireNotNull(donation.id),
                    donation.amount,
                    donation.status,
                    requireNotNull(donation.campaign.id)
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun of(
            donationCount: Int,
            donationTotalAmount: Int,
            donations: List<Donation>
        ): UserDonationRes {
            val items = donations.map(UserDonationItem::from)
            return UserDonationRes(donationCount, donationTotalAmount, items)
        }
    }
}
