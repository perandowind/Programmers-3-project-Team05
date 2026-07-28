package com.team05.petmeeting.domain.naming.repository

import com.team05.petmeeting.domain.naming.entity.AnimalNameCandidate
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface AnimalNameCandidateRepository : JpaRepository<AnimalNameCandidate, Long>, NamingRepositoryCustom {
    fun findByAnimalIdAndProposedName(animalId: Long, proposedName: String): AnimalNameCandidate?
}
