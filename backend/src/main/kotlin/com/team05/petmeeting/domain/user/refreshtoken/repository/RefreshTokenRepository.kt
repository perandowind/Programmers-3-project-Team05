package com.team05.petmeeting.domain.user.refreshtoken.repository

import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.refreshtoken.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    fun deleteByToken(token: UUID)

    fun findByToken(token: UUID): RefreshToken?

    fun deleteAllByUser(user: User)
}
