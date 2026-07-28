package com.team05.petmeeting.domain.feed.dto

import com.team05.petmeeting.domain.feed.enums.FeedCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class FeedReq(
    val category: FeedCategory,

    @field:NotBlank(message = "제목을 입력해주세요.")
    @field:Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
    val title: String,

    @field:NotBlank(message = "내용을 입력해주세요.")
    @field:Size(min = 10, max = 1000, message = "내용은 10자 이상 1000자 이하로 입력해주세요.")
    val content: String,

    val imageUrl: String?,

    val animalId: Long?
)
