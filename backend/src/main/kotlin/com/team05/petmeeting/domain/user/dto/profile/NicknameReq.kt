package com.team05.petmeeting.domain.user.dto.profile

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class NicknameReq(
    @field:NotBlank(message = "닉네임은 필수 입력값입니다.")
    @field:Size(
        min = 1,
        max = 20,
        message = "닉네임은 1~20자 사이여야 합니다."
    )
    val nickname: String
)
