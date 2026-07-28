package com.team05.petmeeting.domain.user.dto.auth.login

data class AccessTokenRes(
    val tokenType: String,
    val accessToken: String
)
