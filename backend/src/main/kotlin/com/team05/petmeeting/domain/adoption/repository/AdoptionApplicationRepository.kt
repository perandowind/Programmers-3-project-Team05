package com.team05.petmeeting.domain.adoption.repository

import com.team05.petmeeting.domain.adoption.entity.AdoptionApplication
import com.team05.petmeeting.domain.adoption.entity.AdoptionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface AdoptionApplicationRepository : JpaRepository<AdoptionApplication, Long> {
    fun findByUser_Id(userId: Long): List<AdoptionApplication>
    fun findByIdAndUser_Id(applicationId: Long, userId: Long): java.util.Optional<AdoptionApplication>
    fun existsByUser_IdAndAnimal_Id(userId: Long, animalId: Long): Boolean
    fun findByAnimal_Shelter_CareRegNo(careRegNo: String): List<AdoptionApplication>
    fun findByUser_IdAndStatus(userId: Long, status: AdoptionStatus): List<AdoptionApplication>

    fun existsByUser_IdAndAnimal_IdAndStatus(
        userId: Long,
        animalId: Long,
        status: AdoptionStatus
    ): Boolean
}
