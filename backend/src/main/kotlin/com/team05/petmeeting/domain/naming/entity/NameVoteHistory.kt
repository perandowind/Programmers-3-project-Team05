package com.team05.petmeeting.domain.naming.entity

import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.user.entity.User
import jakarta.persistence.*
import java.time.LocalDateTime

// 복합 유니크 키: 유저는 한 동물에 한번만 투표가능
// 투표 내역 누적됨 -> 사용하지 않는 수정일은 메모리 낭비
@Entity
@Table(
    name = "name_vote_history",
    uniqueConstraints =
        [UniqueConstraint(
            name = "uk_user_candidate_vote",
            columnNames = ["user_id", "animal_id"]
        )]
)
class NameVoteHistory(

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val user: User, // 투표한 유저

    @JoinColumn(name = "animal_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val animal: Animal,  // 어떤 동물에게 투표했는지

    @JoinColumn(name = "candidate_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val candidate: AnimalNameCandidate // 투표 대상 후보

) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(nullable = false, updatable = false) // 수정 불가능하게 설정
    val votedAt: LocalDateTime = LocalDateTime.now()

}
