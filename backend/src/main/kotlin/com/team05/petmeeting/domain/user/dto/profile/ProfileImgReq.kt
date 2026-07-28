package com.team05.petmeeting.domain.user.dto.profile

import jakarta.validation.constraints.NotBlank

data class ProfileImgReq(
    @field:NotBlank(message = "프로필 이미지 URL을 입력해주세요.")
    val profileImageUrl: String
)
