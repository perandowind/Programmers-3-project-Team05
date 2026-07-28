package com.team05.petmeeting.domain.naming.entity

import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "animal_name_candidate",
    uniqueConstraints =
        [UniqueConstraint(
            name = "uk_animal_name",
            columnNames = ["animal_id", "proposed_name"]
        )]
)
// 이름 후보 (user_id 포함)
class AnimalNameCandidate(
    @JoinColumn(name = "animal_id", nullable = false) // 대상 유기동물 ID
    @ManyToOne(fetch = FetchType.LAZY)
    val animal: Animal,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User, // 최초 작명자

    @Column(nullable = false, length = 10)
    val proposedName: String, // 제안된 이름
) : BaseEntity() {

    @Column(nullable = false)
    var voteCount: Int = 0 // 현재 득표수
        protected set

    // 득표수 증가 메서드
    fun addVoteCount() {
        voteCount++
    }

    @Column(nullable = false)
    var isConfirmed: Boolean = false // 최종 선정 여부
        protected set

    // 이름 확정 메서드
    fun confirmName() {
        isConfirmed = true
    }
}
