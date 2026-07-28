package com.team05.petmeeting.domain.animal.service

import com.team05.petmeeting.domain.animal.dto.AnimalRes
import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.animal.entity.QAnimal.animal
import com.team05.petmeeting.domain.animal.errorcode.AnimalErrorCode
import com.team05.petmeeting.domain.animal.repository.AnimalRepository
import com.team05.petmeeting.global.exception.BusinessException
import jakarta.annotation.PostConstruct
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Service
class AnimalService(
    private val animalRepository: AnimalRepository
) {
    // 종(Key)별로 품종 리스트(Value)를 묶어서 관리하는 동시성 보장 Map 캐시
    private val cachedKindMap: MutableMap<String, List<String>> = ConcurrentHashMap()

    // 애플리케이션 로딩 시점에 실행되어 DB에서 세부품종 목록을 캐싱
    @PostConstruct
    fun initKindCache() {
        val tuples = animalRepository.findDistinctKindFullNames()

        // 1. 임시로 데이터를 그룹화할 가변 맵 준비
        val tempMap = HashMap<String, MutableList<String>>()

        for (tuple in tuples) {
            val upKind = tuple.get(animal.upKindNm) ?: continue
            val kindFull = tuple.get(animal.kindFullNm) ?: continue

            // 종(개/고양이/기타) 그룹이 없으면 새로 만들고 품종을 추가
            tempMap.computeIfAbsent(upKind) { ArrayList() }.add(kindFull)
        }

        // 2. 동시성이 보장되는 메인 캐시 맵에 Thread-safe 리스트로 변환하여 적재
        cachedKindMap.clear()
        tempMap.forEach { (upKind, kindList) ->
            // 품종 이름 가나다순 정렬까지 적용해서 프론트가 쓰기 편하게 제공
            kindList.sort()
            cachedKindMap[upKind] = CopyOnWriteArrayList(kindList)
        }
    }

    // 프론트엔드에게 그룹화된 품종 맵을 반환하는 메서드
    fun getKindFullNames(): Map<String, List<String>> {
        return cachedKindMap
    }

    fun findByAnimalId(animalId: Long): Animal =
        animalRepository.findById(animalId)
            .orElseThrow { BusinessException(AnimalErrorCode.ANIMAL_NOT_FOUND) }


    fun getAnimals(
        region: String?,
        kind: String?,
        kindFullNm: String?,
        stateGroup: Int?,
        pageable: Pageable
    ): Page<AnimalRes> {
        // 예외처리 (페이지 번호 음수)
        if (pageable.pageNumber < 0) {
            throw BusinessException(AnimalErrorCode.INVALID_PAGE_NUMBER)
        }

        // QueryDSL 레포지토리 호출 (DB에서 필터링 + 페이징 완료)
        val animalPage: Page<Animal> =
            animalRepository.findAnimalsWithFilter(region, kind, kindFullNm, stateGroup, pageable)

        // 3. Entity를 DTO로 변환하여 반환
        return animalPage.map(::AnimalRes)
    }

    fun findMatchedAnimals(
        species: String,
        size: String,
        region: String,
        housing: String,
        activity: String,
        experience: String,
        pageable: Pageable
    ): Page<AnimalRes> {
        val animalsPage = animalRepository.findMatched(species, size, region, housing, activity, experience, pageable)

        return animalsPage.map { animal ->
            val reason = getRecommendationReason(animal, experience, housing)

            AnimalRes(animal).copy(recommendationReason = reason)
        }
    }

    fun getRecommendationReason(animal: Animal, experience: String, housing: String): String {
        val specialMark = animal.specialMark ?: ""
        val isBeginner = experience.contains("처음")

        // 아래 내용은 성향 칭찬에서 스킵
        val isAdminMemo = listOf("리더기", "마이크로칩", "등록칩", "이동 제한", "검출", "구조").any { specialMark.contains(it) }

        // 칭찬할 성향 단어
        val friendlyKeywords = listOf("온순", "얌전", "친화", "순함", "애교", "잘따름", "좋아", "활발", "사회성")
        val isExplicitlyFriendly = friendlyKeywords.any { specialMark.contains(it) }

        return when {
            isBeginner -> {
                if (isExplicitlyFriendly && !isAdminMemo) {
                    "🔰 실제 공고상에 \"${specialMark}\"라고 기록될 만큼 친화력이 뛰어나서 초보 보호자님께 안성맞춤이에요!"
                } else {
                    "🔰 실내 적응과 양육이 수월한 표준적인 활동 성향을 가지고 있어 초보 보호자님도 안심하고 함께 시작할 수 있어요."
                }
            }
            else -> {
                if (!isAdminMemo && specialMark.isNotEmpty()) {
                    "🤝 실제 공고 특징인 \"${specialMark}\" 성향을 너른 애정으로 포용하고 이끌어주실 숙련된 보호자님의 깊은 교감이 기대되는 아이예요."
                } else {
                    "🤝 이미 반려동물 동거 경험이 있으신 보호자님의 든든한 사랑과 케어로 더욱 아름다운 인연을 만들어갈 수 있는 친구예요."
                }
            }
        }
    }


}
