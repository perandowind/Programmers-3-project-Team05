package com.team05.petmeeting.domain.naming.repository

import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberPath
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import com.team05.petmeeting.domain.animal.entity.QAnimal.animal
import com.team05.petmeeting.domain.animal.errorcode.AnimalErrorCode
import com.team05.petmeeting.domain.naming.dto.NameCandidateRes
import com.team05.petmeeting.domain.naming.dto.NameCandidateRes.CandidateDto
import com.team05.petmeeting.domain.naming.entity.QAnimalNameCandidate.animalNameCandidate
import com.team05.petmeeting.domain.naming.entity.QNameVoteHistory.nameVoteHistory
import com.team05.petmeeting.domain.shelter.entity.QShelter.shelter
import com.team05.petmeeting.domain.user.entity.QUser
import com.team05.petmeeting.domain.user.entity.QUser.user
import com.team05.petmeeting.global.exception.BusinessException

class NamingRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : NamingRepositoryCustom {


    override fun getCandidates(animalId: Long, userId: Long?): NameCandidateRes {
        // 1. 동물 정보 조회
        val animalEntity = queryFactory
            .selectFrom(animal)
            .where(animal.id.eq(animalId))
            .fetchOne() ?: throw BusinessException(AnimalErrorCode.ANIMAL_NOT_FOUND)

        // 2. 후보 리스트 조회 (CandidateDto 필드 순서 준수)
        val candidateDtoList = queryFactory
            .select(
                Projections.constructor(
                    CandidateDto::class.java,
                    animalNameCandidate.id,
                    animal.id,  // 2번째: animalId
                    animalNameCandidate.proposedName,
                    animalNameCandidate.user.nickname,
                    animalNameCandidate.voteCount,
                    checkIsVoted(animalNameCandidate.id, userId)
                )
            )
            .from(animalNameCandidate)
            .leftJoin(animalNameCandidate.user, user)
            .where(animalNameCandidate.animal.id.eq(animalId))
            .orderBy(
                animalNameCandidate.voteCount.desc(),
                animalNameCandidate.createdAt.asc()
            )
            .fetch()

        return NameCandidateRes(
            animalId,
            animalEntity.name,
            candidateDtoList,
            candidateDtoList.size
        )
    }

    override fun findAllQualifiedCandidatesByShelter(careNm: String, threshold: Int): List<CandidateDto> {

        return queryFactory
            .select(
                Projections.constructor(
                    CandidateDto::class.java,
                    animalNameCandidate.id,
                    animal.id,  // 2번째: animalId
                    animalNameCandidate.proposedName,
                    animalNameCandidate.user.nickname,
                    animalNameCandidate.voteCount,
                    Expressions.asBoolean(false)
                )
            )
            .from(animalNameCandidate)
            .join(animalNameCandidate.animal, animal)
            .join(animal.shelter, shelter)
//            .leftJoin(animalNameCandidate.user, user)  <- 불필요한 조인 주석처리
            .where(
                shelter.careNm.eq(careNm),
                animalNameCandidate.voteCount.goe(threshold), // Querydsl 비교연산자 greater or equal (>=)
                animal.name.isNull()
            )
            .orderBy(
                animal.id.asc(),
                animalNameCandidate.voteCount.desc(),
                animalNameCandidate.createdAt.asc()
            )
            .fetch()
    }

    private fun checkIsVoted(candidateId: NumberPath<Long>, userId: Long?): BooleanExpression {

        if (userId == null) {
            return Expressions.asBoolean(false)
        }

        return JPAExpressions
            .selectOne()
            .from(nameVoteHistory)
            .where(
                nameVoteHistory.candidate.id.eq(candidateId)
                    .and(nameVoteHistory.user.id.eq(userId))
            )
            .exists()
    }
}