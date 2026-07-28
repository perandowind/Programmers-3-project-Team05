package com.team05.petmeeting.domain.user.repository

import com.team05.petmeeting.domain.user.entity.User
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {

    fun findByEmail(email: String): User?

    @Query("select distinct u from User u join fetch u.userAuths where u.email = :email")
    fun findByEmailWithAuths(email: String): User?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    fun findByIdWithLock(@Param("id") id: Long): User?
}