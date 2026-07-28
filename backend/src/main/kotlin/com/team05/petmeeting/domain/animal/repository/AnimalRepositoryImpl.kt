package com.team05.petmeeting.domain.animal.repository

import com.querydsl.core.Tuple
import com.querydsl.core.types.Expression
import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.PathBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.animal.entity.QAnimal.animal
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.util.StringUtils

class AnimalRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : AnimalRepositoryCustom {

    override fun findAnimalsWithFilter(
        region: String?,
        kind: String?,
        kindFullNm: String?,
        stateGroup: Int?,
        pageable: Pageable
    ): Page<Animal> {
        // 데이터처리 쿼리 (페이징)

        val content = queryFactory
            .selectFrom(animal)
            .where(
                stateGroupEq(stateGroup),
                kindFullNmEq(kindFullNm),
                regionStartsWith(region),
                kindEq(kind)
            )
            .offset(pageable.offset) // 페이지 시작위치
            .limit(pageable.pageSize.toLong()) // 페이지 사이즈
            .orderBy(*getOrderSpecifier(pageable.sort)) // 정렬 변환 메서드 사용 -> 코틀린은 배열을 가변 인자로 넘길 때 반드시 스프레드 연산자(*) 사용
            .fetch() // 쿼리 실행결과 -> 리스트

        // 전체 개수 조회 쿼리 (페이징에 필요)
        val total = queryFactory
            .select(animal.count())
            .from(animal)
            .where(
                stateGroupEq(stateGroup),
                kindFullNmEq(kindFullNm),
                regionStartsWith(region),
                kindEq(kind)
            )
            .fetchOne() // 쿼리 실행결과 -> 단일 객체

        return PageImpl(content, pageable, total ?: 0L)
    }

    // 정렬 변환 메서드: Spring Data Sort -> Querydsl OrderSpecifier로 변환

    private fun getOrderSpecifier(sort: Sort): Array<OrderSpecifier<*>> {
        // 1. QClass의 실제 alias를 가져옴 (타입 안정성 확보)
        val pathBuilder = PathBuilder(Animal::class.java, animal.metadata.name)

        // 2. map을 이용해 Sort -> OrderSpecifier로 바로 변환
        return sort.map { order ->
            val direction = if (order.isAscending) Order.ASC else Order.DESC
            val targetPath = pathBuilder.get(order.property) as Expression<out Comparable<*>>

            OrderSpecifier(direction, targetPath)
        }.toList().toTypedArray()
    }

    override fun findDistinctKindFullNames(): List<Tuple> {
        return queryFactory
            .select(animal.upKindNm, animal.kindFullNm)
            .from(animal)
            .where(
                animal.upKindNm.isNotNull,
                animal.kindFullNm.isNotNull
            )
            .distinct()
            .fetch()
    }

    // null 처리하는 동적 메서드
    // contains -> LIKE '%경남%' | startsWith -> LIKE '경남%'
    // 공고번호(noticeNo)의 앞부분이 지역명으로 시작하는 API 특성을 활용하여 지역 필터링 | 경남-진주-2024-00124
    private fun kindFullNmEq(kindFullNm: String?): BooleanExpression? =
        if (StringUtils.hasText(kindFullNm)) animal.kindFullNm.eq(kindFullNm) else null

    private fun regionStartsWith(region: String?): BooleanExpression? =
        if (StringUtils.hasText(region)) animal.noticeNo.startsWith(region) else null

    private fun kindEq(kind: String?): BooleanExpression? =
        if (StringUtils.hasText(kind)) animal.upKindNm.eq(kind) else null

    private fun stateGroupEq(stateGroup: Int?): BooleanExpression? =
        stateGroup?.let { animal.stateGroup.eq(it) }

    override fun findMatched(
        species: String,
        size: String,
        region: String,
        housing: String,
        activity: String,
        experience: String,
        pageable: Pageable
    ): Page<Animal> {
        val kindFilter = if (species == "전체") null else kindEq(species)
        val sizeFilter = sizeCheck(size)

        val content = queryFactory
            .selectFrom(animal)
            .where(
                kindFilter,
                regionKeywordsLike(region),
                beginnerSafetyCheck(experience),
                sizeFilter,
                animal.stateGroup.eq(0) // 기존에 존재하는 idx_animal_state_notice 인덱스를 타기 위해 processState 대신 stateGroup=0 사용
            )
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .orderBy(animal.noticeEdt.asc())
            .fetch()

        // 160만 건 테이블을 풀스캔하는 최악의 병목(Count 쿼리)을 완전히 제거합니다!
        // 추천 시스템은 프론트엔드에서 LIMIT 6으로 1페이지만 요청하므로 전체 개수가 필요 없습니다.
        val total = content.size.toLong()

        return PageImpl(content, pageable, total)
    }

    private fun sizeCheck(size: String?): BooleanExpression? {
        if (!StringUtils.hasText(size) || size == "상관없음") return null

        // Hibernate 6의 엄격한 HQL 파싱을 우회하기 위해 명시적인 HQL 타입(double)으로 캐스팅합니다.
        val numericWeight = Expressions.numberTemplate(
            Double::class.javaObjectType,
            "cast(replace({0}, '(Kg)', '') as double)",
            animal.weight
        )

        return when (size) {
            "소형 (품에 쏙 들어오는 크기)" -> numericWeight.loe(7.0)
            "중형 (어디서나 당당한 크기)" -> numericWeight.gt(7.0).and(numericWeight.lt(15.0))
            "대형 (든든하고 듬직한 크기)" -> numericWeight.goe(15.0)
            else -> null
        }
    }

    private fun regionKeywordsLike(region: String?): BooleanExpression? {
        if (!StringUtils.hasText(region) || region == "전국 어디든") return null
        val keywords = when (region) {
            "서울/경기/인천" -> listOf("서울", "경기", "인천")
            "강원/충청" -> listOf("강원", "충북", "충남", "대전", "세종")
            "경상/부산/대구" -> listOf("경북", "경남", "부산", "대구", "울산")
            "전라/제주" -> listOf("전북", "전남", "광주", "제주")
            else -> return null
        }

        var expression: BooleanExpression? = null
        for (keyword in keywords) {
            val nextExpr = animal.noticeNo.startsWith(keyword) // careAddr의 contains 대신 noticeNo의 startsWith(전방 일치) 사용
            expression = expression?.or(nextExpr) ?: nextExpr
        }
        return expression
    }

    private fun beginnerSafetyCheck(experience: String?): BooleanExpression? {
        if (experience == null || !experience.contains("처음")) return null
        val safetyKeywords = listOf("경계", "공격", "입질", "사나", "도망", "예민", "사망", "피해", "물림")

        var safeExpression: BooleanExpression? = null
        for (keyword in safetyKeywords) {
            val nextExpr = animal.specialMark.contains(keyword).not()
            safeExpression = safeExpression?.and(nextExpr) ?: nextExpr
        }

        return animal.specialMark.isNull.or(safeExpression)
    }

}
