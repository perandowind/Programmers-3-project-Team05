package com.team05.petmeeting.domain.animal.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "animal_sync_states")
class SyncState protected constructor() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_type", nullable = false, unique = true, length = 20)
    lateinit var syncType: AnimalSyncType
        protected set

    @Column(name = "last_updated_at")
    var lastUpdatedAt: LocalDateTime? = null
        protected set

    private constructor(syncType: AnimalSyncType) : this() {
        this.syncType = syncType
    }

    // 동기화 타입별 마지막 성공 시각을 갱신한다.
    fun updateLastUpdatedAt(lastUpdatedAt: LocalDateTime) {
        this.lastUpdatedAt = lastUpdatedAt
    }

    companion object {
        // sync type 별 상태 엔터티를 처음 생성할 때 사용한다.
        @JvmStatic
        fun create(syncType: AnimalSyncType): SyncState = SyncState(syncType)
    }
}
