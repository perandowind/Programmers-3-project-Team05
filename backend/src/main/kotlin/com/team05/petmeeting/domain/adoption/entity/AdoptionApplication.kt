package com.team05.petmeeting.domain.adoption.entity

import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.user.entity.User
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
@Table(name = "adoption_applications")
class AdoptionApplication protected constructor() : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id", nullable = false)
    lateinit var animal: Animal
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    lateinit var status: AdoptionStatus
        protected set

    @Column(name = "apply_reason", nullable = false, length = 1000)
    lateinit var applyReason: String
        protected set

    @Column(name = "apply_tel", nullable = false, length = 30)
    lateinit var applyTel: String
        protected set

    @Column(name = "reviewed_at")
    var reviewedAt: LocalDateTime? = null
        protected set

    @Column(name = "rejection_reason", length = 1000)
    var rejectionReason: String? = null
        protected set

    private constructor(
        user: User,
        animal: Animal,
        applyReason: String,
        applyTel: String,
    ) : this() {
        this.user = user
        this.animal = animal
        status = AdoptionStatus.Processing
        this.applyReason = applyReason
        this.applyTel = applyTel
    }

    fun approve() {
        status = AdoptionStatus.Approved
        reviewedAt = LocalDateTime.now()
        rejectionReason = null
    }

    fun reject(rejectionReason: String) {
        status = AdoptionStatus.Rejected
        reviewedAt = LocalDateTime.now()
        this.rejectionReason = rejectionReason
    }

    fun markProcessing() {
        status = AdoptionStatus.Processing
        reviewedAt = null
        rejectionReason = null
    }

    companion object {
        @JvmStatic
        fun create(
            user: User,
            animal: Animal,
            applyReason: String,
            applyTel: String,
        ): AdoptionApplication = AdoptionApplication(user, animal, applyReason, applyTel)
    }
}
