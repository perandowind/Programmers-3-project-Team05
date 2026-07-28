package com.team05.petmeeting.domain.donation.repository

import com.team05.petmeeting.domain.donation.entity.Donation
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DonationRepository : JpaRepository<Donation, Long> {
    fun findByUser_Id(userId: Long): List<Donation>

    @EntityGraph(attributePaths = ["campaign"])
    fun findByPaymentId(s: String): Donation?

    // 프론트 / 웹훅 금액 중복 증감 처리
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Donation d WHERE d.paymentId = :paymentId")
    fun findByPaymentIdForUpdate(@Param("paymentId") paymentId: String): Donation?
}
