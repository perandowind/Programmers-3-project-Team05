package com.team05.petmeeting.domain.ads.entity

import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.shelter.entity.Shelter
import com.team05.petmeeting.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "ads_post_requests")
class AdsPostRequest protected constructor() : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id", nullable = false)
    lateinit var animal: Animal
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "care_reg_no", nullable = false)
    lateinit var shelter: Shelter
        protected set

    @Column(name = "image_url", nullable = false, length = 1000)
    lateinit var imageUrl: String
        protected set

    @Column(nullable = false, length = 1000)
    lateinit var caption: String
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    lateinit var status: AdsPostStatus
        protected set

    @Column(name = "reviewed_at")
    var reviewedAt: LocalDateTime? = null
        protected set

    @Column(name = "published_at")
    var publishedAt: LocalDateTime? = null
        protected set

    @Column(name = "rejection_reason", length = 1000)
    var rejectionReason: String? = null
        protected set

    private constructor(
        animal: Animal,
        shelter: Shelter,
        imageUrl: String,
        caption: String,
    ) : this() {
        this.animal = animal
        this.shelter = shelter
        this.imageUrl = imageUrl
        this.caption = caption
        status = AdsPostStatus.Processing
    }

    fun approve() {
        status = AdsPostStatus.Approved
        reviewedAt = LocalDateTime.now()
        rejectionReason = null
    }

    fun markPublished() {
        status = AdsPostStatus.Published
        publishedAt = LocalDateTime.now()
    }

    fun reject(rejectionReason: String?) {
        status = AdsPostStatus.Rejected
        reviewedAt = LocalDateTime.now()
        publishedAt = null
        this.rejectionReason = rejectionReason?.takeIf { it.isNotBlank() }
    }

    fun markProcessing() {
        status = AdsPostStatus.Processing
        reviewedAt = null
        publishedAt = null
        rejectionReason = null
    }

    fun replaceCardNews(imageUrl: String, caption: String) {
        this.imageUrl = imageUrl
        this.caption = caption
    }

    companion object {
        fun create(
            animal: Animal,
            shelter: Shelter,
            imageUrl: String,
            caption: String,
        ): AdsPostRequest = AdsPostRequest(animal, shelter, imageUrl, caption)
    }
}
