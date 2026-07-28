package com.team05.petmeeting.domain.animal.repository

import com.team05.petmeeting.domain.animal.entity.Animal
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

// JpaRepository 기본 기능 + Custom 기능 모두 보유
interface AnimalRepository : JpaRepository<Animal, Long>, AnimalRepositoryCustom {
    // 유기번호로 조회
    fun findByDesertionNo(desertionNo: String): Optional<Animal>

    fun findAllByDesertionNoIn(desertionNos: Collection<String>): List<Animal>

    fun existsByDesertionNo(desertionNo: String): Boolean

    // 원자적 UPDATE
    @Modifying(clearAutomatically = true) // 영속성 컨텍스트를 비워주는 설정 -> 쿼리 사용후 캐시삭제
    @Query(
        "UPDATE Animal a SET a.totalCheerCount = a.totalCheerCount + 1 " +
                "WHERE a.id = :animalId"
    )
    fun incrementCheerCount(@Param("animalId") animalId: Long)

    // 보호중인 동물만 응원 수 기준 상위 N개 조회 (stateGroup = 0)
    fun findAllByStateGroupOrderByTotalCheerCountDesc(stateGroup: Int, pageable: Pageable): List<Animal>
}
