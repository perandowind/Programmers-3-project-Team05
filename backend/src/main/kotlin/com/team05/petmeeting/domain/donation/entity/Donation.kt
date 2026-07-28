package com.team05.petmeeting.domain.donation.entity

import com.team05.petmeeting.domain.campaign.entity.Campaign
import com.team05.petmeeting.domain.donation.enums.DonationStatus
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "donations")
class Donation (
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    var campaign: Campaign,

    @Column(unique = true)
    var paymentId: String,

    var amount: Int = 0,

    @Enumerated(EnumType.STRING)
    var status: DonationStatus = DonationStatus.PENDING

) : BaseEntity() {

    fun complete() {
        this.status = DonationStatus.PAID
    }

    fun fail() {
        this.status = DonationStatus.FAILED
    }

    companion object {
        fun create(user: User, campaign: Campaign, paymentId: String, amount: Int): Donation {
            return Donation(
                user,
                campaign,
                paymentId,
                amount
            )
        }
    }
}
