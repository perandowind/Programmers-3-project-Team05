package com.team05.petmeeting.domain.user.dto.auth.emailstart

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class EmailStartReq(
    @field:Email
    @field:NotBlank
    val email: String
)
