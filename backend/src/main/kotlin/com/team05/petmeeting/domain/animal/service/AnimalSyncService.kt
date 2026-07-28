package com.team05.petmeeting.domain.animal.service

import com.team05.petmeeting.domain.animal.config.AnimalSyncProperties
import com.team05.petmeeting.domain.animal.dto.AnimalSyncRes
import com.team05.petmeeting.domain.animal.dto.external.AnimalApiResponse
import com.team05.petmeeting.domain.animal.dto.external.AnimalItem
import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.animal.entity.AnimalSyncType
import com.team05.petmeeting.domain.animal.entity.SyncState
import com.team05.petmeeting.domain.animal.errorcode.AnimalErrorCode
import com.team05.petmeeting.domain.animal.repository.AnimalRepository
import com.team05.petmeeting.domain.animal.repository.SyncStateRepository
import com.team05.petmeeting.domain.shelter.dto.ShelterCommand
import com.team05.petmeeting.domain.shelter.entity.Shelter
import com.team05.petmeeting.domain.shelter.repository.ShelterRepository
import com.team05.petmeeting.domain.shelter.service.ShelterService
import com.team05.petmeeting.global.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.*
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

@Service
class AnimalSyncService(
    private val animalExternalService: AnimalExternalService,
    private val animalRepository: AnimalRepository,
    private val syncStateRepository: SyncStateRepository,
    private val shelterRepository: ShelterRepository,
    private val shelterService: ShelterService,
    private val animalSyncProperties: AnimalSyncProperties,
    private val animalService: AnimalService,
) {
    private data class SyncPageResult(
        val message: String,
        val savedCount: Int,
        val elapsedMs: Long,
    )

    // 특정 페이지를 한 번 조회해 동물 데이터를 저장한다.
    fun fetchAndSaveAnimals(pageNo: Int, numOfRows: Int): AnimalSyncRes {
        validatePageNo(pageNo)
        validateNumOfRows(numOfRows)

        try {
            val result = fetchAndInsertAnimals(pageNo, numOfRows, null, null, Int.MAX_VALUE)

            // 단일 페이지 적재 성공 후 캐시 리프레시
            animalService.initKindCache()

            log.info(
                "Animal sync completed: pageNo={}, numOfRows={}, savedCount={}, elapsedMs={}",
                pageNo,
                numOfRows,
                result.savedCount,
                result.elapsedMs,
            )
            return AnimalSyncRes(result.message, result.savedCount, result.elapsedMs)
        } catch (e: BusinessException) {
            throw e
        } catch (e: RuntimeException) {
            log.error("Animal sync failed: pageNo={}, numOfRows={}", pageNo, numOfRows, e)
            throw BusinessException(AnimalErrorCode.ANIMAL_SYNC_FAILED, e)
        }
    }

    // 2008년 1월부터 현재까지 월 단위로 나눠 최초 적재를 수행한다.
    fun runInitialMonthlySync(numOfRows: Int): AnimalSyncRes {
        validateNumOfRows(numOfRows)

        try {
            val startedAt = Instant.now()
            val savedCount = fetchAndSaveMonthlyAnimalsFromStartDate(numOfRows, Int.MAX_VALUE)
            // 모든 월별 적재가 정상 종료된 뒤에만 최초 적재 성공 시각을 갱신한다.
            updateSyncState(AnimalSyncType.INITIAL)

            // 최초 대용량 적재 완료 후 캐시 리프레시
            animalService.initKindCache()

            val elapsedMs = elapsedMs(startedAt)
            log.info(
                "Initial animal sync completed: from={}, to={}, numOfRows={}, savedCount={}, elapsedMs={}",
                animalSyncProperties.initial.startDate,
                LocalDate.now(),
                numOfRows,
                savedCount,
                elapsedMs,
            )
            return AnimalSyncRes(INITIAL_SYNC_MESSAGE, savedCount, elapsedMs)
        } catch (e: BusinessException) {
            throw e
        } catch (e: RuntimeException) {
            log.error("Initial animal sync failed: numOfRows={}", numOfRows, e)
            throw BusinessException(AnimalErrorCode.INITIAL_ANIMAL_SYNC_FAILED, e)
        }
    }

    // 마지막 업데이트 시점 이후 수정된 데이터를 다시 반영한다.
    fun runUpdateSync(numOfRows: Int): AnimalSyncRes {
        validateNumOfRows(numOfRows)

        try {
            val startedAt = Instant.now()
            val bgupd = getUpdateStartDate()
            val enupd = LocalDate.now()
            val savedCount = fetchAndSaveAnimalsByUpdatedDate(bgupd, enupd, numOfRows)
            // 업데이트 반영이 끝난 경우에만 다음 업데이트 기준 시각을 갱신한다.
            updateSyncState(AnimalSyncType.UPDATE)

            // 정기 업데이트 반영 완료 후 캐시 리프레시
            animalService.initKindCache()

            val elapsedMs = elapsedMs(startedAt)
            log.info(
                "Animal update sync completed: from={}, to={}, numOfRows={}, savedCount={}, elapsedMs={}",
                bgupd,
                enupd,
                numOfRows,
                savedCount,
                elapsedMs,
            )
            return AnimalSyncRes(UPDATE_SYNC_MESSAGE, savedCount, elapsedMs)
        } catch (e: BusinessException) {
            throw e
        } catch (e: RuntimeException) {
            log.error("Animal update sync failed: numOfRows={}", numOfRows, e)
            throw BusinessException(AnimalErrorCode.UPDATE_ANIMAL_SYNC_FAILED, e)
        }
    }

    // 최초 적재 범위를 월별로 순회하면서 저장 건수를 누적한다.
    fun fetchAndSaveMonthlyAnimalsFromStartDate(numOfRows: Int, maxSaveCount: Int): Int {
        val today = LocalDate.now()
        var currentMonthStart = animalSyncProperties.initial.startDate.withDayOfMonth(1)
        var totalSavedCount = 0

        while (!currentMonthStart.isAfter(today) && totalSavedCount < maxSaveCount) {
            var currentMonthEnd = currentMonthStart.withDayOfMonth(currentMonthStart.lengthOfMonth())
            if (currentMonthEnd.isAfter(today)) {
                currentMonthEnd = today
            }

            totalSavedCount += fetchAndSaveAnimalsByDateRange(
                currentMonthStart,
                currentMonthEnd,
                numOfRows,
                maxSaveCount - totalSavedCount,
            )
            currentMonthStart = currentMonthStart.plusMonths(1).withDayOfMonth(1)
        }

        return totalSavedCount
    }

    // 수정일 기준 조회 결과를 페이지 단위로 끝까지 저장하거나 갱신한다.
    fun fetchAndSaveAnimalsByUpdatedDate(bgupd: LocalDate, enupd: LocalDate, numOfRows: Int): Int {
        var pageNo = 1
        var totalSavedCount = 0

        while (true) {
            val response = fetchUpdatedPageWithRetry(pageNo, numOfRows, bgupd, enupd)
            val items = extractItems(response)

            if (items.isEmpty()) {
                break
            }

            totalSavedCount += saveOrUpdateAnimals(items)
            pageNo++
            waitForNextUpdatePage()
        }

        return totalSavedCount
    }

    // 업데이트 페이지 조회는 일시적 외부 장애에 한해 제한 횟수만큼 재시도한다.
    private fun fetchUpdatedPageWithRetry(
        pageNo: Int,
        numOfRows: Int,
        bgupd: LocalDate,
        enupd: LocalDate,
    ): AnimalApiResponse? {
        val maxRetryCount = animalSyncProperties.update.maxRetryCount

        repeat(maxRetryCount) { attempt ->
            try {
                return animalExternalService.fetchAnimalsByUpdatedDate(pageNo, numOfRows, bgupd, enupd)
            } catch (e: RuntimeException) {
                val retryAttempt = attempt + 1
                if (retryAttempt >= maxRetryCount) {
                    throw e
                }

                log.warn(
                    "Animal update sync page fetch failed, retrying: pageNo={}, numOfRows={}, from={}, to={}, attempt={}/{}",
                    pageNo,
                    numOfRows,
                    bgupd,
                    enupd,
                    retryAttempt,
                    maxRetryCount,
                    e,
                )
                waitForRetry()
            }
        }

        return null
    }

    // 일반 조회 API를 한 번 호출해 현재 페이지 저장 결과를 만든다.
    private fun fetchAndInsertAnimals(
        pageNo: Int,
        numOfRows: Int,
        bgnde: LocalDate?,
        endde: LocalDate?,
        maxSaveCount: Int,
    ): SyncPageResult {
        val startedAt = Instant.now()
        val response = animalExternalService.fetchAnimals(pageNo, numOfRows, bgnde, endde)
        val items = extractItems(response)

        if (items.isEmpty()) {
            return SyncPageResult(SYNC_PAGE_MESSAGE, 0, elapsedMs(startedAt))
        }

        val savedCount = saveNewAnimals(items, maxSaveCount)
        return SyncPageResult(SYNC_PAGE_MESSAGE, savedCount, elapsedMs(startedAt))
    }

    // 한 달 범위를 페이지별로 조회하면서 저장 가능한 동물을 누적한다.
    private fun fetchAndSaveAnimalsByDateRange(
        bgnde: LocalDate,
        endde: LocalDate,
        numOfRows: Int,
        maxSaveCount: Int,
    ): Int {
        var pageNo = 1
        var totalSavedCount = 0

        while (totalSavedCount < maxSaveCount) {
            val result = fetchAndInsertAnimals(pageNo, numOfRows, bgnde, endde, maxSaveCount - totalSavedCount)
            if (result.savedCount == 0) {
                break
            }

            totalSavedCount += result.savedCount
            pageNo++
        }

        return totalSavedCount
    }

    // 최초 적재에서는 보호소를 먼저 저장한 뒤, 현재 페이지의 신규 동물만 저장한다.
    private fun saveNewAnimals(items: List<AnimalItem>, maxSaveCount: Int): Int {
        syncShelters(items)

        val animalsToSave = mutableListOf<Animal>()
        val seenDesertionNos = mutableSetOf<String>()

        for (item in items) {
            if (animalsToSave.size >= maxSaveCount) {
                break
            }

            val desertionNo = item.desertionNo
            if (desertionNo.isNullOrBlank()) {
                continue
            }

            if (!seenDesertionNos.add(desertionNo)) {
                continue
            }

            // 보호소가 이미 저장돼 있으면 동물과 연관관계도 함께 연결한다.
            val animal = Animal.from(item)
            assignShelter(animal, item)
            animalsToSave.add(animal)
        }

        animalRepository.saveAll(animalsToSave)
        return animalsToSave.size
    }

    // 업데이트 적재에서는 보호소를 먼저 저장한 뒤, 기존 동물은 수정하고 없으면 새로 저장한다.
    private fun saveOrUpdateAnimals(items: List<AnimalItem>): Int {
        syncShelters(items)

        var savedCount = 0
        for (item in items) {
            val desertionNo = item.desertionNo
            if (desertionNo.isNullOrBlank()) {
                continue
            }

            animalRepository.findByDesertionNo(desertionNo)
                .ifPresentOrElse(
                    { animal ->
                        animal.updateFrom(item)
                        // 기존 동물도 최신 보호소 정보에 맞춰 다시 연결한다.
                        assignShelter(animal, item)
                        animalRepository.save(animal)
                    },
                    {
                        // 새 동물 저장 시에도 보호소 FK를 함께 채운다.
                        val animal = Animal.from(item)
                        assignShelter(animal, item)
                        animalRepository.save(animal)
                    },
                )
            savedCount++
        }

        return savedCount
    }

    // 동물 저장 전에 보호소 정보를 먼저 upsert 한다.
    private fun syncShelters(items: List<AnimalItem>) {
        val shelterCmds = items.asSequence()
            .filter { !it.careRegNo.isNullOrBlank() }
            .filter { !it.updTm.isNullOrBlank() }
            .map {
                val careRegNo = it.careRegNo
                val updatedAt = parseApiUpdatedAt(it.updTm)
                if (updatedAt == null) {
                    log.warn("Skipping shelter sync for careRegNo={} due to invalid updTm={}", careRegNo, it.updTm)
                    return@map null
                }

                ShelterCommand(
                    requireNotNull(careRegNo),
                    it.careNm,
                    it.careTel,
                    it.careAddr,
                    it.careOwnerNm,
                    it.orgNm,
                    updatedAt,
                )
            }
            .filterNotNull()
            .distinct()
            .toList()

        shelterService.createOrUpdateShelters(shelterCmds)
    }

    // careRegNo로 보호소를 찾아 Animal.shelter 연관관계를 연결한다.
    private fun assignShelter(animal: Animal, item: AnimalItem) {
        val careRegNo = item.careRegNo
        if (careRegNo.isNullOrBlank()) {
            return
        }

        val shelter: Shelter? = shelterRepository.findById(careRegNo).orElse(null)
        if (shelter != null) {
            animal.assignShelter(shelter)
        }
    }

    // 외부 API 응답에서 실제 동물 목록만 꺼내고, 비정상 구조면 빈 리스트를 돌려준다.
    private fun extractItems(response: AnimalApiResponse?): List<AnimalItem> {
        val animalResponse = response?.response ?: return emptyList()
        val body = animalResponse.body ?: return emptyList()
        val items = body.items ?: return emptyList()
        return items.item ?: emptyList()
    }

    // 마지막 UPDATE 동기화 시각을 기준으로 다음 업데이트 시작 날짜를 정한다.
    private fun getUpdateStartDate(): LocalDate {
        val lastUpdatedAt = syncStateRepository.findBySyncType(AnimalSyncType.UPDATE)
            .map { it.lastUpdatedAt }
            .orElse(null)

        return lastUpdatedAt?.toLocalDate() ?: animalSyncProperties.initial.startDate
    }

    // 외부 API 페이지 번호는 1부터 시작하므로 0 이하 요청을 차단한다.
    private fun validatePageNo(pageNo: Int) {
        if (pageNo < 1) {
            throw BusinessException(AnimalErrorCode.INVALID_PAGE_NUMBER)
        }
    }

    // 한 번에 조회할 건수가 없으면 적재를 시작할 수 없으므로 사전에 차단한다.
    private fun validateNumOfRows(numOfRows: Int) {
        if (numOfRows < 1) {
            throw BusinessException(AnimalErrorCode.INVALID_SYNC_REQUEST)
        }
    }

    // 동기화가 끝난 시각을 sync type 별로 저장한다.
    private fun updateSyncState(syncType: AnimalSyncType) {
        val syncState = syncStateRepository.findBySyncType(syncType)
            .orElseGet { SyncState.create(syncType) }

        syncState.updateLastUpdatedAt(LocalDateTime.now())
        syncStateRepository.save(syncState)
    }

    // 업데이트 API를 연속 호출할 때 외부 서버 부담을 줄이기 위해 잠시 대기한다.
    private fun waitForNextUpdatePage() {
        try {
            Thread.sleep(animalSyncProperties.update.delayMs)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("업데이트 동기화 대기 중 인터럽트가 발생했습니다.", e)
        }
    }

    // 재시도 전 잠시 대기해 외부 API의 일시적 오류에 바로 연속 타격하지 않도록 한다.
    private fun waitForRetry() {
        try {
            Thread.sleep(animalSyncProperties.update.delayMs)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("업데이트 동기화 재시도 대기 중 인터럽트가 발생했습니다.", e)
        }
    }

    // 시작 시각부터 현재까지 걸린 시간을 밀리초로 계산한다.
    private fun elapsedMs(startedAt: Instant): Long =
        Duration.between(startedAt, Instant.now()).toMillis()

    private fun parseApiUpdatedAt(updTm: String?): LocalDateTime? {
        if (updTm.isNullOrBlank()) {
            return null
        }

        return try {
            LocalDateTime.parse(updTm.trim(), API_UPDATE_TIME_FORMATTER)
        } catch (_: DateTimeException) {
            null
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AnimalSyncService::class.java)
        private val API_UPDATE_TIME_FORMATTER = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .toFormatter()
        private const val SYNC_PAGE_MESSAGE = "유기동물 데이터 동기화 완료"
        private const val INITIAL_SYNC_MESSAGE = "INITIAL_MONTHLY_SYNC_OK"
        private const val UPDATE_SYNC_MESSAGE = "UPDATE_SYNC_OK"
    }
}
