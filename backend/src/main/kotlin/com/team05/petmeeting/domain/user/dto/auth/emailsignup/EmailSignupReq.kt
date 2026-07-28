package com.team05.petmeeting.domain.user.dto.auth.emailsignup

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.UUID

data class EmailSignupReq(

    @field:NotBlank(message = "토큰은 필수 입력값입니다.")
    @field:UUID(message = "잘못된 토큰 형식입니다.")
    val verificationToken: String,

    @field:NotBlank(message = "password는 필수 입력값입니다.")
    @field:Size(min = 8, max = 16, message = "password는 8~16자 사이여야 합니다.")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/]).+$",
        message = "password는 대문자, 소문자, 숫자, 특수문자를 모두 포함해야 합니다."
    )
    val password: String,

    @field:NotBlank(message = "닉네임은 필수 입력값입니다.")
    val nickname: String,

    @field:NotBlank(message = "실명은 필수 입력값입니다.")
    val realname: String
)