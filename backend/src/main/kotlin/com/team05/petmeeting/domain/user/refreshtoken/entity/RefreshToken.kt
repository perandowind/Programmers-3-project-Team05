package com.team05.petmeeting.domain.user.refreshtoken.entity

import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime
import java.util.UUID

@Entity
class RefreshToken protected constructor() : BaseEntity() {

    @field:Column(nullable = false, unique = true)
    lateinit var token: UUID
        protected set

    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User
        protected set

    @field:Column(nullable = false)
    lateinit var expiresAt: LocalDateTime
        protected set

    fun isExpired(): Boolean =
        expiresAt.isBefore(LocalDateTime.now())

    companion object {
        @JvmStatic
        fun create(user: User, token: UUID): RefreshToken =
            RefreshToken().apply {
                this.user = user
                this.token = token
                this.expiresAt = LocalDateTime.now().plusDays(7)
            }
    }
}
