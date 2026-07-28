package com.team05.petmeeting.domain.user.dto.auth.login.local

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class EmailLoginReq(
    @field:NotBlank(message = "이메일은 필수 입력값입니다.")
    @field:Email
    val email: String,

    @field:NotBlank(message = "password는 필수 입력값입니다.")
    @field:Size(
        min = 8,
        max = 16,
        message = "password는 8~16자 사이여야 합니다."
    )
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/]).+$",
        message = "password는 대문자, 소문자, 숫자, 특수문자를 모두 포함해야 합니다."
    )
    val password: String
)
