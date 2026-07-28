package com.team05.petmeeting.domain.cheer.repository

import com.team05.petmeeting.domain.cheer.entity.Cheer
import com.team05.petmeeting.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CheerRepository : JpaRepository<Cheer, Long> {
    fun countByUser(user: User): Long

    @Query("SELECT COUNT(DISTINCT c.animal) From Cheer c where c.user = :user")
    fun countDistinctAnimalByUser(user: User): Long

    @Query("SELECT c.animal, COUNT(c) FROM Cheer c where c.user = :user GROUP BY c.animal")
    fun findCheerCountsByUser(user: User): List<Array<Any?>>
}
