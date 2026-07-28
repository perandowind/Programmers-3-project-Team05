package com.team05.petmeeting.domain.user.dto.profile

import com.team05.petmeeting.domain.user.entity.User

data class UserSummaryRes(
    val nickname: String,
    val name: String,
    val profileImageUrl: String
) {
    companion object {
        @JvmStatic
        fun from(user: User): UserSummaryRes {
            return UserSummaryRes(
                user.nickname,
                user.realname,
                user.profileImageUrl
            )
        }
    }
}
