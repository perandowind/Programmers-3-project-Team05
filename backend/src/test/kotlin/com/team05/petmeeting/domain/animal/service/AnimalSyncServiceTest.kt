package com.team05.petmeeting.domain.animal.service

import com.team05.petmeeting.domain.animal.config.AnimalSyncProperties
import com.team05.petmeeting.domain.animal.dto.AnimalSyncRes
import com.team05.petmeeting.domain.animal.dto.external.*
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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class AnimalSyncServiceTest {

    @InjectMocks
    private lateinit var animalSyncService: AnimalSyncService

    @Mock
    private lateinit var animalExternalService: AnimalExternalService

    @Mock
    private lateinit var animalRepository: AnimalRepository

    @Mock
    private lateinit var syncStateRepository: SyncStateRepository

    @Mock
    private lateinit var shelterRepository: ShelterRepository

    @Mock
    private lateinit var shelterService: ShelterService

    @Mock
    private lateinit var animalService: AnimalService

    @Spy
    private var animalSyncProperties: AnimalSyncProperties = AnimalSyncProperties()

    @Test
    @DisplayName("업데이트 동기화 - 마지막 UPDATE 시점 이후 데이터를 기존 동물 갱신과 신규 동물 저장으로 반영한다")
    fun runUpdateSync_updatesExistingAndCreatesNewAnimal() {
        val lastUpdatedAt = LocalDateTime.of(2026, 4, 20, 13, 30)
        val today = LocalDate.now()
        val updateState = SyncState.create(AnimalSyncType.UPDATE).apply {
            updateLastUpdatedAt(lastUpdatedAt)
        }

        val updatedItem = animalItem(
            desertionNo = "D-001",
            processState = "종료(입양)",
            noticeNo = "NOTICE-UPDATED",
            noticeEdt = "20260430",
            happenPlace = "새 발견장소",
            kindFullNm = "믹스견",
            careNm = "새 보호소",
            careTel = "010-2222-3333",
            updTm = "2026-04-21 10:00:00.0",
            careRegNo = "CARE-001",
        )
        val newItem = animalItem(
            desertionNo = "D-002",
            processState = "보호중",
            noticeNo = "NOTICE-NEW",
            noticeEdt = "20260430",
            happenPlace = "신규 발견장소",
            kindFullNm = "코리안숏헤어",
            careNm = "새 보호소",
            careTel = "010-2222-3333",
            updTm = "2026-04-21 11:00:00.0",
            careRegNo = "CARE-001",
        )

        val existingAnimal = Animal.from(
            animalItem(
                desertionNo = "D-001",
                processState = "보호중",
                noticeNo = "NOTICE-OLD",
                noticeEdt = "20260410",
                happenPlace = "기존 발견장소",
                kindFullNm = "믹스견",
                careNm = "기존 보호소",
                careTel = "010-1111-2222",
                updTm = "2026-04-19 09:00:00.0",
                careRegNo = "CARE-OLD",
            ),
        )
        val shelter = Shelter.create(
            ShelterCommand(
                "CARE-001",
                "새 보호소",
                "010-2222-3333",
                "보호소 주소",
                "담당자",
                "기관",
                LocalDateTime.of(2026, 4, 21, 10, 0),
            ),
        )

        `when`(syncStateRepository.findBySyncType(AnimalSyncType.UPDATE)).thenReturn(Optional.of(updateState))
        `when`(
            animalExternalService.fetchAnimalsByUpdatedDate(
                eq(1),
                eq(10),
                eq(lastUpdatedAt.toLocalDate()),
                eq(today),
            ),
        ).thenReturn(apiResponse(listOf(updatedItem, newItem)))
        `when`(
            animalExternalService.fetchAnimalsByUpdatedDate(
                eq(2),
                eq(10),
                eq(lastUpdatedAt.toLocalDate()),
                eq(today),
            ),
        ).thenReturn(apiResponse(emptyList()))
        `when`(animalRepository.findByDesertionNo("D-001")).thenReturn(Optional.of(existingAnimal))
        `when`(animalRepository.findByDesertionNo("D-002")).thenReturn(Optional.empty())
        `when`(shelterRepository.findById("CARE-001")).thenReturn(Optional.of(shelter))

        val response: AnimalSyncRes = animalSyncService.runUpdateSync(10)

        assertThat(response.message()).isEqualTo("UPDATE_SYNC_OK")
        assertThat(response.savedCount()).isEqualTo(2)

        assertThat(existingAnimal.processState).isEqualTo("종료(입양)")
        assertThat(existingAnimal.noticeNo).isEqualTo("NOTICE-UPDATED")
        assertThat(existingAnimal.happenPlace).isEqualTo("새 발견장소")
        assertThat(existingAnimal.careNm).isEqualTo("새 보호소")
        assertThat(existingAnimal.shelter).isEqualTo(shelter)

        val animalCaptor = ArgumentCaptor.forClass(Animal::class.java)
        verify(animalRepository, times(2)).save(animalCaptor.capture())
        val savedAnimals = animalCaptor.allValues
        assertThat(savedAnimals)
            .extracting<String> { it.desertionNo }
            .containsExactlyInAnyOrder("D-001", "D-002")
        assertThat(
            savedAnimals.first { it.desertionNo == "D-002" }.shelter,
        ).isEqualTo(shelter)

        verify(shelterService).createOrUpdateShelters(anyList<ShelterCommand>())
        verify(syncStateRepository).save(updateState)
        assertThat(updateState.lastUpdatedAt).isAfter(lastUpdatedAt)
    }

    @Test
    @DisplayName("업데이트 동기화 - updTm에 소수점이 없어도 정상 반영한다")
    fun runUpdateSync_acceptsUpdTmWithoutFraction() {
        val lastUpdatedAt = LocalDateTime.of(2026, 4, 20, 13, 30)
        val today = LocalDate.now()
        val updateState = SyncState.create(AnimalSyncType.UPDATE).apply {
            updateLastUpdatedAt(lastUpdatedAt)
        }
        val item = animalItem(
            desertionNo = "D-003",
            processState = "보호중",
            noticeNo = "NOTICE-003",
            noticeEdt = "20260430",
            happenPlace = "발견장소",
            kindFullNm = "믹스견",
            careNm = "보호소",
            careTel = "010-1234-5678",
            updTm = "2026-04-21 10:00:00",
            careRegNo = "CARE-003",
        )
        val shelter = Shelter.create(
            ShelterCommand(
                "CARE-003",
                "보호소",
                "010-1234-5678",
                "보호소 주소",
                "담당자",
                "기관",
                LocalDateTime.of(2026, 4, 21, 10, 0),
            ),
        )

        `when`(syncStateRepository.findBySyncType(AnimalSyncType.UPDATE)).thenReturn(Optional.of(updateState))
        `when`(
            animalExternalService.fetchAnimalsByUpdatedDate(
                eq(1),
                eq(10),
                eq(lastUpdatedAt.toLocalDate()),
                eq(today),
            ),
        ).thenReturn(apiResponse(listOf(item)))
        `when`(
            animalExternalService.fetchAnimalsByUpdatedDate(
                eq(2),
                eq(10),
                eq(lastUpdatedAt.toLocalDate()),
                eq(today),
            ),
        ).thenReturn(apiResponse(emptyList()))
        `when`(animalRepository.findByDesertionNo("D-003")).thenReturn(Optional.empty())
        `when`(shelterRepository.findById("CARE-003")).thenReturn(Optional.of(shelter))

        val response = animalSyncService.runUpdateSync(10)

        assertThat(response.message()).isEqualTo("UPDATE_SYNC_OK")
        assertThat(response.savedCount()).isEqualTo(1)
        verify(animalRepository).save(any(Animal::class.java))
        verify(shelterService).createOrUpdateShelters(anyList<ShelterCommand>())
    }

    @Test
    @DisplayName("업데이트 동기화 - 마지막 UPDATE 시점이 없으면 2008-01-01부터 조회하고 상태를 새로 저장한다")
    fun runUpdateSync_withoutPreviousStateStartsFromInitialDate() {
        val today = LocalDate.now()
        `when`(syncStateRepository.findBySyncType(AnimalSyncType.UPDATE)).thenReturn(Optional.empty())
        `when`(
            animalExternalService.fetchAnimalsByUpdatedDate(
                eq(1),
                eq(20),
                eq(LocalDate.of(2025, 1, 1)),
                eq(today),
            ),
        ).thenReturn(apiResponse(emptyList()))

        val response = animalSyncService.runUpdateSync(20)

        assertThat(response.message()).isEqualTo("UPDATE_SYNC_OK")
        assertThat(response.savedCount()).isZero()

        val syncStateCaptor = ArgumentCaptor.forClass(SyncState::class.java)
        verify(syncStateRepository).save(syncStateCaptor.capture())
        assertThat(syncStateCaptor.value.syncType).isEqualTo(AnimalSyncType.UPDATE)
        assertThat(syncStateCaptor.value.lastUpdatedAt).isNotNull()
        verify(animalRepository, never()).save(any(Animal::class.java))
        verify(shelterService, never()).createOrUpdateShelters(anyList<ShelterCommand>())
    }

    @Test
    @DisplayName("업데이트 동기화 - 외부 API 오류가 발생하면 업데이트 실패 예외로 변환하고 상태를 갱신하지 않는다")
    fun runUpdateSync_whenExternalApiFailsThrowsBusinessException() {
        val lastUpdatedAt = LocalDateTime.of(2026, 4, 20, 13, 30)
        val today = LocalDate.now()
        val updateState = SyncState.create(AnimalSyncType.UPDATE).apply {
            updateLastUpdatedAt(lastUpdatedAt)
        }

        `when`(syncStateRepository.findBySyncType(AnimalSyncType.UPDATE)).thenReturn(Optional.of(updateState))
        `when`(
            animalExternalService.fetchAnimalsByUpdatedDate(
                eq(1),
                eq(10),
                eq(lastUpdatedAt.toLocalDate()),
                eq(today),
            ),
        ).thenThrow(IllegalStateException("외부 API 오류"))

        assertThatThrownBy { animalSyncService.runUpdateSync(10) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(AnimalErrorCode.UPDATE_ANIMAL_SYNC_FAILED)

        verify(syncStateRepository, never()).save(any(SyncState::class.java))
        verify(animalRepository, never()).save(any(Animal::class.java))
    }

    @Test
    @DisplayName("업데이트 동기화 - 외부 API가 일시 실패하면 재시도 후 성공 시 적재를 이어간다")
    fun runUpdateSync_retriesTransientExternalApiFailure() {
        val lastUpdatedAt = LocalDateTime.of(2026, 4, 20, 13, 30)
        val today = LocalDate.now()
        val updateState = SyncState.create(AnimalSyncType.UPDATE).apply {
            updateLastUpdatedAt(lastUpdatedAt)
        }
        val newItem = animalItem(
            desertionNo = "D-002",
            processState = "보호중",
            noticeNo = "NOTICE-NEW",
            noticeEdt = "20260430",
            happenPlace = "신규 발견장소",
            kindFullNm = "코리안숏헤어",
            careNm = "새 보호소",
            careTel = "010-2222-3333",
            updTm = "2026-04-21 11:00:00.0",
            careRegNo = "CARE-001",
        )
        val shelter = Shelter.create(
            ShelterCommand(
                "CARE-001",
                "새 보호소",
                "010-2222-3333",
                "보호소 주소",
                "담당자",
                "기관",
                LocalDateTime.of(2026, 4, 21, 10, 0),
            ),
        )

        `when`(syncStateRepository.findBySyncType(AnimalSyncType.UPDATE)).thenReturn(Optional.of(updateState))
        `when`(
            animalExternalService.fetchAnimalsByUpdatedDate(
                eq(1),
                eq(10),
                eq(lastUpdatedAt.toLocalDate()),
                eq(today),
            ),
        )
            .thenThrow(IllegalStateException("일시 오류"))
            .thenReturn(apiResponse(listOf(newItem)))
        `when`(
            animalExternalService.fetchAnimalsByUpdatedDate(
                eq(2),
                eq(10),
                eq(lastUpdatedAt.toLocalDate()),
                eq(today),
            ),
        ).thenReturn(apiResponse(emptyList()))
        `when`(animalRepository.findByDesertionNo("D-002")).thenReturn(Optional.empty())
        `when`(shelterRepository.findById("CARE-001")).thenReturn(Optional.of(shelter))

        val response = animalSyncService.runUpdateSync(10)

        assertThat(response.message()).isEqualTo("UPDATE_SYNC_OK")
        assertThat(response.savedCount()).isEqualTo(1)
        verify(animalExternalService, times(2)).fetchAnimalsByUpdatedDate(
            eq(1),
            eq(10),
            eq(lastUpdatedAt.toLocalDate()),
            eq(today),
        )
        verify(syncStateRepository).save(updateState)
    }

    @Test
    @DisplayName("페이지 동기화 - 잘못된 페이지 번호면 적재를 시작하지 않고 검증 예외를 던진다")
    fun fetchAndSaveAnimals_withInvalidPageNoThrowsBusinessException() {
        assertThatThrownBy { animalSyncService.fetchAndSaveAnimals(0, 10) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(AnimalErrorCode.INVALID_PAGE_NUMBER)

        verify(animalExternalService, never()).fetchAnimals(anyInt(), anyInt(), any(), any())
    }

    @Test
    @DisplayName("초기 적재 - 잘못된 조회 건수면 적재를 시작하지 않고 검증 예외를 던진다")
    fun runInitialMonthlySync_withInvalidNumOfRowsThrowsBusinessException() {
        assertThatThrownBy { animalSyncService.runInitialMonthlySync(0) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(AnimalErrorCode.INVALID_SYNC_REQUEST)

        verify(animalExternalService, never()).fetchAnimals(anyInt(), anyInt(), any(), any())
        verify(syncStateRepository, never()).save(any(SyncState::class.java))
    }

    private fun apiResponse(itemList: List<AnimalItem>): AnimalApiResponse {
        // 외부 API DTO는 setter가 없어 테스트에서는 reflection으로 중첩 응답 구조를 조립한다.
        val items = AnimalItems()
        ReflectionTestUtils.setField(items, "item", itemList)

        val body = AnimalBody()
        ReflectionTestUtils.setField(body, "items", items)

        val response = AnimalResponse()
        ReflectionTestUtils.setField(response, "body", body)

        val apiResponse = AnimalApiResponse()
        ReflectionTestUtils.setField(apiResponse, "response", response)
        return apiResponse
    }

    private fun animalItem(
        desertionNo: String,
        processState: String,
        noticeNo: String,
        noticeEdt: String,
        happenPlace: String,
        kindFullNm: String,
        careNm: String,
        careTel: String,
        updTm: String,
        careRegNo: String,
    ): AnimalItem {
        // 실제 API 응답과 같은 모양으로 필드를 채워 Animal.from/updateFrom 테스트 데이터를 만든다.
        val item = AnimalItem()
        ReflectionTestUtils.setField(item, "desertionNo", desertionNo)
        ReflectionTestUtils.setField(item, "processState", processState)
        ReflectionTestUtils.setField(item, "noticeNo", noticeNo)
        ReflectionTestUtils.setField(item, "noticeEdt", noticeEdt)
        ReflectionTestUtils.setField(item, "happenPlace", happenPlace)
        ReflectionTestUtils.setField(item, "upKindNm", "개")
        ReflectionTestUtils.setField(item, "kindFullNm", kindFullNm)
        ReflectionTestUtils.setField(item, "colorCd", "흰색")
        ReflectionTestUtils.setField(item, "age", "2024(년생)")
        ReflectionTestUtils.setField(item, "weight", "5(Kg)")
        ReflectionTestUtils.setField(item, "sexCd", "M")
        ReflectionTestUtils.setField(item, "popfile1", "https://example.com/image1.jpg")
        ReflectionTestUtils.setField(item, "popfile2", "https://example.com/image2.jpg")
        ReflectionTestUtils.setField(item, "specialMark", "온순함")
        ReflectionTestUtils.setField(item, "careOwnerNm", "담당자")
        ReflectionTestUtils.setField(item, "careNm", careNm)
        ReflectionTestUtils.setField(item, "careAddr", "보호소 주소")
        ReflectionTestUtils.setField(item, "careTel", careTel)
        ReflectionTestUtils.setField(item, "updTm", updTm)
        ReflectionTestUtils.setField(item, "careRegNo", careRegNo)
        ReflectionTestUtils.setField(item, "orgNm", "기관")
        return item
    }
}
