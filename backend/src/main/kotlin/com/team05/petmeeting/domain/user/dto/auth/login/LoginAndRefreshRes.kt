package com.team05.petmeeting.domain.user.dto.auth.login

data class LoginAndRefreshRes(
    val refreshToken: String,
    val accessTokenRes: AccessTokenRes
)
