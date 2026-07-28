package com.team05.petmeeting.domain.user.dto.auth.emailverify

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class EmailVerifyReq(
    @field:Email
    @field:NotBlank
    val email: String,

    @field:Pattern(regexp = "\\d{6}", message = "인증 코드는 6자리 숫자여야 합니다.")
    @field:NotBlank
    val code: String
)
