package com.team05.petmeeting.domain.animal.controller

import com.team05.petmeeting.domain.animal.dto.AnimalRes
import com.team05.petmeeting.domain.animal.service.AnimalService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/animals")
@Tag(name = "AnimalController", description = "동물 조회 API")
class AnimalController(
    private val animalService: AnimalService
) {
    companion object {
        const val DEFAULT_PAGE_SIZE = 12
    }

    data class PageResBody<T>(
        val content: List<T>,
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
        val last: Boolean
    )


    @GetMapping
    @Operation(summary = "유기동물 필터 적용 조회", description = "필터(지역, 축종, 상태)와 페이징/정렬을 지원합니다.")
    fun animalList(
        @RequestParam(required = false) region: String?,
        @RequestParam(required = false) kind: String?,
        @RequestParam(required = false) kindFullNm: String?,
        @RequestParam(required = false) stateGroup: Int?,
        @PageableDefault(page = 0, size = DEFAULT_PAGE_SIZE, sort = ["noticeEdt"], direction = Sort.Direction.ASC) pageable: Pageable
    ): ResponseEntity<PageResBody<AnimalRes>> {
        val page: Page<AnimalRes> = animalService.getAnimals(region, kind, kindFullNm, stateGroup, pageable)

        val response = PageResBody(
            content = page.content,  // 현재 페이지에 해당하는 List<AnimalRes>
            page = page.number,  // 현재 보고 있는 페이지 번호 (0부터 시작)
            size = page.size,  // 페이지 크기 ex.20
            totalElements = page.totalElements,  // 전체 개수   ex.342
            totalPages = page.totalPages,  // 전체 페이지 ex.18
            last = page.isLast // 마지막 페이지 여부
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{animalId}")
    @Operation(summary = "유기동물 상세 조회")
    fun animalDetail(
        @PathVariable animalId: Long
    ): ResponseEntity<AnimalRes> {
        val animal = animalService.findByAnimalId(animalId)
        val response = AnimalRes(animal)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/kinds")
    @Operation(summary = "드롭다운 채우기용 품종 목록 조회", description = "DB를 조회하지 않고 서버 메모리(캐시)에서 즉시 반환합니다.")
    fun getAnimalKinds(): ResponseEntity<Map<String, List<String>>> {
        val kindMap = animalService.getKindFullNames()
        return ResponseEntity.ok(kindMap)
    }

    @GetMapping("/recommendations")
    @Operation(summary = "설문 추천 동물 목록 조회")
    fun getRecommendations(
        @RequestParam species: String,
        @RequestParam(name = "animalSize") size: String,
        @RequestParam region: String,
        @RequestParam housing: String,
        @RequestParam activity: String,
        @RequestParam experience: String,
        pageable: Pageable
    ): ResponseEntity<PageResBody<AnimalRes>> {
        val page: Page<AnimalRes> = animalService.findMatchedAnimals(
            species,        // 개 고양이 전체
            size,           // 소형 중형 대형 상관없음
            region,         // "서울/경기/인천" "강원/충청" "경상/부산/대구" "전라/제주" "전국 어디든"
            housing,        // "아파트/원룸 (실내 생활 위주)" "단독주택/마당 (활동 범위가 넓은 환경)"
            activity,       // "매일 산책 가능 (활동적인 편)"  "주말/가끔 가능 (차분하고 정적인 편)"
            experience,     // "처음 키워보는 초보 집사"  "키워본 적 있는 숙련된 집사"
            pageable
        )

        val response = PageResBody(
            content = page.content,  // 현재 페이지에 해당하는 List<AnimalRes>
            page = page.number,  // 현재 보고 있는 페이지 번호 (0부터 시작)
            size = page.size,  // 페이지 크기 ex.20
            totalElements = page.totalElements,  // 전체 개수   ex.342
            totalPages = page.totalPages,  // 전체 페이지 ex.18
            last = page.isLast // 마지막 페이지 여부
        )

        return ResponseEntity.ok(response)
    }
}
