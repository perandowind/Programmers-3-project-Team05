package com.team05.petmeeting.domain.user.dto.profile

import com.team05.petmeeting.domain.feed.entity.Feed
import java.time.format.DateTimeFormatter

data class UserFeedRes(
    val totalFeedCount: Long,
    val feeds: List<ProfileFeedItem>
) {

    data class ProfileFeedItem(
        val feedId: Long,
        val category: String,
        val title: String,
        val createdAt: String
    ) {
        companion object {
            @JvmStatic
            fun from(feed: Feed): ProfileFeedItem {
                return ProfileFeedItem(
                    requireNotNull(feed.id),
                    feed.category.toString(),
                    feed.title,
                    requireNotNull(feed.createdAt).format(DateTimeFormatter.ofPattern("yyyy. MM. dd."))
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun of(totalFeedCount: Long, feedList: List<Feed>): UserFeedRes {
            val items = feedList.map(ProfileFeedItem::from)
            return UserFeedRes(totalFeedCount, items)
        }
    }
}
