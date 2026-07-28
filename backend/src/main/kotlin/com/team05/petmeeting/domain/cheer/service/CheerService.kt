package com.team05.petmeeting.domain.cheer.service

import com.team05.petmeeting.domain.animal.errorcode.AnimalErrorCode
import com.team05.petmeeting.domain.animal.repository.AnimalRepository
import com.team05.petmeeting.domain.cheer.dto.CheerRes
import com.team05.petmeeting.domain.cheer.dto.CheerStatusDto
import com.team05.petmeeting.domain.cheer.entity.Cheer
import com.team05.petmeeting.domain.cheer.errorcode.CheerErrorCode
import com.team05.petmeeting.domain.cheer.repository.CheerRepository
import com.team05.petmeeting.domain.user.errorcode.UserErrorCode
import com.team05.petmeeting.domain.user.repository.UserRepository
import com.team05.petmeeting.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class CheerService(
    private val cheerRepository: CheerRepository,
    private val userRepository: UserRepository,
    private val animalRepository: AnimalRepository,
) {
    companion object {
        private const val MAX_DAILY_CHEER_COUNT = 5
    }

    // 오늘 응원 상태 조회
    fun getTodaysStatus(userId: Long): CheerStatusDto {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(UserErrorCode.USER_NOT_FOUND) }

        // 초기화 필요하면 응원 개수 초기화
        user.resetDailyHeartCountIfNeeded()

        val usedToday = user.dailyHeartCount
        val remainingToday = MAX_DAILY_CHEER_COUNT - usedToday

        // 내일 자정 계산 (DB에 저장하지 않고, 매번 계산해서 사용)
        val tomorrow_midnight = LocalDate.now().plusDays(1) // 2026-04-13
            .atStartOfDay() // 00:00:00
        val resetAt = tomorrow_midnight.toString()

        return CheerStatusDto(usedToday, remainingToday, resetAt)
    }

    // 응원 부여
    fun cheerAnimal(userId: Long, animalId: Long): CheerRes {
        // [유저만 비관적 락으로 조회] -> 조회와 동시에 X-Lock 획득되어 유저 광클 차단
        val user = userRepository.findByIdWithLock(userId)
            ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)
        // 동물 조회
        val animal = animalRepository.findById(animalId)
            .orElseThrow{ BusinessException(AnimalErrorCode.ANIMAL_NOT_FOUND) }

        // 초기화 필요하면 응원 개수 초기화
        user.resetDailyHeartCountIfNeeded()

        // 5회 제한 확인
        if (user.dailyHeartCount >= MAX_DAILY_CHEER_COUNT) {
            throw BusinessException(CheerErrorCode.DAILY_CHEER_LIMIT_EXCEEDED)
        }

        // [1단계: 유저 도메인 처리]
        user.useDailyCheer()
        userRepository.saveAndFlush(user)

        // [2단계: 동물 도메인 처리]
        // 벌크 연산으로 인해 1차 캐시가 비워짐 (유저 정보는 이미 DB에 flush되었으므로 안전)
        animalRepository.incrementCheerCount(animalId) // 동물 Row에 배타 락(X-Lock) 획득

        // [3단계: 응원 도메인 처리]
        // 외래 키 제약 조건으로 인해 Animal과 User에 S-Lock을 시도하지만,
        // 이미 같은 트랜잭션 내에서 강력한 X-Lock을 쥐고 있으므로 경합 없이 무사 통과
        val cheer = Cheer(user, animal)
        cheerRepository.save(cheer)


        // 캐시가 비워졌으므로 다시 조회
        val updatedAnimal = animalRepository.findById(animalId).get()
        val updatedUser = userRepository.findById(userId).get()

        return CheerRes(
            animalId,
            updatedAnimal.totalCheerCount,
            updatedAnimal.getTemperature(),
            5 - updatedUser.dailyHeartCount
        )
    }

}

