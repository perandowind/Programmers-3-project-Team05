package com.team05.petmeeting.domain.feed.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class FeedLikeRes(
     val likeCount: Int,

     @get:JsonProperty("isLiked")
     val isLiked: Boolean
)