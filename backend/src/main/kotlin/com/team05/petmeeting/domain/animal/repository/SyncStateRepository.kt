package com.team05.petmeeting.domain.animal.repository

import com.team05.petmeeting.domain.animal.entity.AnimalSyncType
import com.team05.petmeeting.domain.animal.entity.SyncState
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface SyncStateRepository : JpaRepository<SyncState, Long> {
    // 특정 동기화 타입의 마지막 성공 시각 저장 엔터티를 조회한다.
    fun findBySyncType(syncType: AnimalSyncType): Optional<SyncState>
}
