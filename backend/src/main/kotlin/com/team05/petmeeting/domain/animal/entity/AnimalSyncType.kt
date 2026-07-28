package com.team05.petmeeting.domain.animal.entity

// 동기화 상태를 최초 적재와 업데이트 적재로 구분한다.
enum class AnimalSyncType {
    INITIAL,
    UPDATE,
}
