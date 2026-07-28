package com.team05.petmeeting.domain.user.event

data class SignupOtpMailRequestedEvent(
    val email: String,
    val code: String,
)
