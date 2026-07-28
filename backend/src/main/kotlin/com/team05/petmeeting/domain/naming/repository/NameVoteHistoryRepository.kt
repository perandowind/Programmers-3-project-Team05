package com.team05.petmeeting.domain.naming.repository

import com.team05.petmeeting.domain.naming.entity.NameVoteHistory
import org.springframework.data.jpa.repository.JpaRepository

interface NameVoteHistoryRepository : JpaRepository<NameVoteHistory, Long> {
    fun existsByUserIdAndAnimalId(userId: Long, animalId: Long): Boolean
}
