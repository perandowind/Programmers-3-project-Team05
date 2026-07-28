package com.team05.petmeeting.domain.user.dto.profile

data class MyProfileDetailRes(
    val feedCount: Long,
    val cheerCount: Long,
    val cheerAnimalCount: Long,
    val feedCommentCount: Long,
    val animalCommentCount: Long
) {
    companion object {
        @JvmStatic
        fun of(
            feedCount: Long,
            cheerCount: Long,
            cheerAnimalCount: Long,
            feedCommentCount: Long,
            animalCommentCount: Long
        ): MyProfileDetailRes {
            return MyProfileDetailRes(feedCount, cheerCount, cheerAnimalCount, feedCommentCount, animalCommentCount)
        }
    }
}
