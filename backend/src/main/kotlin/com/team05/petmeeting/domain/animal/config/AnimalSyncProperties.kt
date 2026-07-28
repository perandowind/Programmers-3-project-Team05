package com.team05.petmeeting.domain.animal.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.LocalDate
import jakarta.validation.constraints.NotBlank

@Validated
@ConfigurationProperties(prefix = "animal.sync")
class AnimalSyncProperties {
    // 일반 동기화(/sync) 요청의 기본 페이지 번호
    var defaultPageNo: Int = 1
    // 일반 동기화(/sync) 요청의 기본 조회 건수
    var defaultNumOfRows: Int = 10
    // 내부 동기화 API 호출용 secret
    @field:NotBlank
    var secret: String = ""
    // 최초 적재(/sync/initial) 기본 설정
    var initial: Initial = Initial()
    // 업데이트 적재(/sync/update) 기본 설정
    var update: Update = Update()

    class Initial {
        // 월별 최초 적재를 시작할 기준일
        var startDate: LocalDate = LocalDate.of(2025, 1, 1)
        // 최초 적재(/sync/initial) 요청의 기본 조회 건수
        var numOfRows: Int = 500
    }

    class Update {
        // 업데이트 적재(/sync/update) 요청의 기본 조회 건수
        var numOfRows: Int = 500
        // 업데이트 페이지 호출 간 대기 시간
        var delayMs: Long = 300L
        // 업데이트 조회 실패 시 최대 재시도 횟수
        var maxRetryCount: Int = 3
    }
}
