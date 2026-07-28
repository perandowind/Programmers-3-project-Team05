package com.team05.petmeeting.domain.user.dto.profile

import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.provider.Provider
import java.time.LocalDateTime

data class UserProfileRes(
    val profileImageUrl: String,
    val nickname: String,
    val email: String,
    val name: String,
    val createdAt: LocalDateTime,
    val hasLocalAuth: Boolean,
) {
    companion object {
        @JvmStatic
        fun from(user: User): UserProfileRes {
            return UserProfileRes(
                user.profileImageUrl,
                user.nickname,
                user.email,
                user.realname,
                requireNotNull(user.createdAt),
                user.userAuths.any { it.provider == Provider.LOCAL },
            )
        }

        private fun maskEmail(email: String): String {
            if (!email.contains("@")) {
                return email
            }

            val parts = email.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val local = parts[0]
            val domain: String? = parts[1]

            if (local.length <= 2) {
                return local.get(0).toString() + "*@" + domain
            }

            val masked = StringBuilder()
            masked.append(local.get(0))
            for (i in 1..<local.length - 1) {
                masked.append("*")
            }
            masked.append(local.get(local.length - 1))

            return "$masked@$domain"
        }
    }
}
