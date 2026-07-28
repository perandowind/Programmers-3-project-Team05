package com.team05.petmeeting.domain.animal.controller

import com.team05.petmeeting.domain.animal.config.AnimalSyncProperties
import com.team05.petmeeting.domain.animal.dto.AnimalSyncRes
import com.team05.petmeeting.domain.animal.service.AnimalSyncService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/animals")
class AnimalSyncController(
    private val animalSyncService: AnimalSyncService,
    private val animalSyncProperties: AnimalSyncProperties,
) {
    // 특정 페이지와 건수로 유기동물 데이터를 한 번 조회해 저장한다.
    @PostMapping("/sync")
    fun syncAnimals(
        @RequestParam(required = false) pageNo: Int?,
        @RequestParam(required = false) numOfRows: Int?,
    ): ResponseEntity<AnimalSyncRes> =
        ResponseEntity.ok(
            animalSyncService.fetchAndSaveAnimals(
                pageNo ?: animalSyncProperties.defaultPageNo,
                numOfRows ?: animalSyncProperties.defaultNumOfRows,
            ),
        )

    // 2008년 1월부터 현재까지 월 단위 최초 적재를 수행한다.
    @PostMapping("/sync/initial")
    fun syncInitialMonthly(
        @RequestParam(required = false) numOfRows: Int?,
    ): ResponseEntity<AnimalSyncRes> =
        ResponseEntity.ok(animalSyncService.runInitialMonthlySync(numOfRows ?: animalSyncProperties.initial.numOfRows))

    // 마지막 성공 시각 이후 수정된 데이터만 다시 반영한다.
    @PostMapping("/sync/update")
    fun syncByUpdatedDate(
        @RequestParam(required = false) numOfRows: Int?,
    ): ResponseEntity<AnimalSyncRes> =
        ResponseEntity.ok(animalSyncService.runUpdateSync(numOfRows ?: animalSyncProperties.update.numOfRows))
}
