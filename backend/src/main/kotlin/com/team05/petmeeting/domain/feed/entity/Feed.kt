package com.team05.petmeeting.domain.feed.entity

import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.comment.entity.FeedComment
import com.team05.petmeeting.domain.feed.enums.FeedCategory
import com.team05.petmeeting.domain.feed.errorcode.FeedErrorCode
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.global.entity.BaseEntity
import com.team05.petmeeting.global.exception.BusinessException
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "feeds")
class Feed protected constructor() : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    lateinit var user: User
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    var animal: Animal? = null
        protected set

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    lateinit var category: FeedCategory
        protected set

    @Column(nullable = false)
    lateinit var title: String
        protected set

    @Column(nullable = false, columnDefinition = "TEXT")
    lateinit var content: String
        protected set

    var imageUrl: String? = null
        protected set

    @OneToMany(mappedBy = "feed", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val comments: MutableList<FeedComment> = mutableListOf()

    constructor(
        user: User,
        category: FeedCategory,
        title: String,
        content: String,
        imageUrl: String?,
        animal: Animal?
    ) : this() {
        this.user = user
        this.category = category
        this.title = title
        this.content = content
        this.imageUrl = imageUrl
        this.animal = animal
    }

    fun update(category: FeedCategory, title: String, content: String, imageUrl: String?, animal: Animal?) {
        this.category = category
        this.title = title
        this.content = content
        this.imageUrl = imageUrl
        this.animal = animal
    }

    // 테스트용 생성자
    constructor(category: FeedCategory, title: String, content: String, imageUrl: String?) : this() {
        this.category = category
        this.title = title
        this.content = content
        this.imageUrl = imageUrl
    }

    fun checkModify(user: User) {
        if (requireNotNull(this.user.id) != requireNotNull(user.id)) {
            throw BusinessException(FeedErrorCode.FORBIDDEN)
        }
    }

    fun checkDelete(user: User) {
        if (requireNotNull(this.user.id) != requireNotNull(user.id)) {
            throw BusinessException(FeedErrorCode.FORBIDDEN)
        }
    }
}
