package com.team05.petmeeting.domain.user.dto.profile

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class PasswordReq(

    @field:NotBlank(message = "현재 비밀번호를 입력해주세요.")
    val currentPassword: String,

    @field:NotBlank(message = "새 비밀번호를 입력해주세요.")
    @field:Size(min = 8, max = 16, message = "비밀번호는 8~16자 사이여야 합니다.")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/]).+$",
        message = "비밀번호는 대문자, 소문자, 숫자, 특수문자를 모두 포함해야 합니다."
    )
    val newPassword: String
)
