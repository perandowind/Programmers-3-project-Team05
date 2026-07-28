package com.team05.petmeeting.domain.feed.entity

import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.global.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "feed_likes")
class FeedLike protected constructor() : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    lateinit var user: User
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    lateinit var feed: Feed
        protected set

    constructor(user: User, feed: Feed) : this() {
        this.user = user
        this.feed = feed
    }
}
