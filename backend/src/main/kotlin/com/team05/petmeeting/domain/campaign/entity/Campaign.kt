package com.team05.petmeeting.domain.campaign.entity

import com.team05.petmeeting.domain.campaign.enums.CampaignStatus
import com.team05.petmeeting.domain.shelter.entity.Shelter
import com.team05.petmeeting.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "campaigns")
class Campaign(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "care_reg_no")
    var shelter: Shelter,

    var title: String,

    var description: String,

    var targetAmount: Int

) : BaseEntity() {

    var currentAmount: Int = 0

    @Enumerated(EnumType.STRING)
    var status: CampaignStatus = CampaignStatus.ACTIVE

    protected constructor() : this(
        shelter = UNINITIALIZED_SHELTER,
        title = "",
        description = "",
        targetAmount = 0
    )

    fun addAmount(amount: Int) {
        currentAmount += amount

        if (currentAmount >= targetAmount) {
            status = CampaignStatus.COMPLETE
        }
    }

    fun close() {
        status = CampaignStatus.CLOSED
    }

    companion object {
        private val UNINITIALIZED_SHELTER = Shelter(
            "",
            null,
            null,
            null,
            null,
            null,
            java.time.LocalDateTime.MIN
        )

        fun create(
            shelter: Shelter,
            title: String,
            description: String,
            targetAmount: Int
        ): Campaign {
            return Campaign(
                shelter,
                title,
                description,
                targetAmount
            )
        }
    }
}