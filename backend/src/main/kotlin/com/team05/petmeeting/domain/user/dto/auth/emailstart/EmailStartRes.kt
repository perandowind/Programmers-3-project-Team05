package com.team05.petmeeting.domain.user.dto.auth.emailstart

data class EmailStartRes(
    val exists: Boolean,
    val nextStep: NextStep
) {
    enum class NextStep {
        SIGNUP_WITH_OTP, LOGIN_PASSWORD, SOCIAL_LOGIN_ONLY
    }
}
