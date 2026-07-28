package com.team05.petmeeting.domain.comment.entity

import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "animal_comments")
class AnimalComment (
    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var user: User,

    @JoinColumn(name = "animal_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var animal: Animal,

    @Column(length = 255, nullable = false) var content: String
) : BaseEntity() {

    fun updateContent(newContent: String) {
        this.content = newContent
    }

    companion object {
        @JvmStatic
        fun create(user: User, animal: Animal, content: String): AnimalComment {
            val comment = AnimalComment(
                user = user,
                animal = animal,
                content = content
            )

            animal.comments.add(comment)

            return comment
        }
    }
}