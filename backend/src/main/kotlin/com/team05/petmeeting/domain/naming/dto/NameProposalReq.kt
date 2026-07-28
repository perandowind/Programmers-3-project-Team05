package com.team05.petmeeting.domain.naming.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class NameProposalReq(
    @field:NotBlank(message = "이름은 필수 입력 값입니다.")
    @field:Size(
        min = 1,
        max = 10,
        message = "이름은 1자에서 10자 사이여야 합니다."
    )
    @field:Pattern(regexp = "^[가-힣]+$", message = "이름은 공백이나 숫자, 영문 없이 한글만 입력 가능합니다.")
    val proposedName: String
)
