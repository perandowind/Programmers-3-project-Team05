package com.team05.petmeeting.domain.comment.repository

import com.team05.petmeeting.domain.comment.entity.AnimalComment
import com.team05.petmeeting.domain.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface AnimalCommentRepository : JpaRepository<AnimalComment, Long> {
    fun findByAnimal_Id(animalId: Long): MutableList<AnimalComment>

    fun findByAnimal_Id(animalId: Long, pageable: Pageable): Page<AnimalComment>

    fun findAllByUserOrderByCreatedAtDesc(user: User): List<AnimalComment>

    fun countAnimalCommentByUser(user: User): Long
}

