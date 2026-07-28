package com.team05.petmeeting.domain.comment.entity

import com.team05.petmeeting.domain.feed.entity.Feed
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "feed_comments")
class FeedComment (
    @JoinColumn(name = "user_id",nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var user: User, @JoinColumn(name = "feed_id", nullable = false)

    @ManyToOne(fetch = FetchType.LAZY)
    var feed: Feed,

    @Column(length = 255, nullable = false)
    var content: String
) : BaseEntity() {
    fun updateContent(newContent: String) {
        this.content = newContent
    }

    companion object {
        @JvmStatic
        fun create(user: User, feed: Feed, content: String): FeedComment {
            val comment = FeedComment(
                user = user,
                feed = feed,
                content = content
            )
            feed.comments.add(comment)

            return comment
        }
    }
}
