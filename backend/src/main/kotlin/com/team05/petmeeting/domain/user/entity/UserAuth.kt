package com.team05.petmeeting.domain.user.entity

import com.team05.petmeeting.domain.user.provider.Provider
import com.team05.petmeeting.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "user_auths",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_auths_provider_provider_id",
            columnNames = ["provider", "providerId"],
        ),
    ],
)
class UserAuth protected constructor() : BaseEntity() {

    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User
        internal set

    @field:Enumerated(EnumType.STRING)
    @field:Column(nullable = false)
    lateinit var provider: Provider
        protected set

    @field:Column(nullable = false)
    lateinit var providerId: String
        protected set

    @field:Column(nullable = true)
    var password: String? = null
        protected set

    fun updatePassword(password: String) {
        this.password = password
    }

    companion object {
        @JvmStatic
        fun create(provider: Provider, providerId: String, password: String?): UserAuth =
            UserAuth().apply {
                this.provider = provider
                this.providerId = providerId
                this.password = password
            }
    }
}
