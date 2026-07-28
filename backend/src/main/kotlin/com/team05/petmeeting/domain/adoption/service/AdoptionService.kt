package com.team05.petmeeting.domain.adoption.service

import com.team05.petmeeting.domain.adoption.dto.AdoptionApplyReq
import com.team05.petmeeting.domain.adoption.dto.AdoptionApplyRes
import com.team05.petmeeting.domain.adoption.dto.AdoptionDetailRes
import com.team05.petmeeting.domain.adoption.dto.toApplyRes
import com.team05.petmeeting.domain.adoption.dto.toDetailRes
import com.team05.petmeeting.domain.adoption.entity.AdoptionApplication
import com.team05.petmeeting.domain.adoption.errorcode.AdoptionErrorCode
import com.team05.petmeeting.domain.adoption.repository.AdoptionApplicationRepository
import com.team05.petmeeting.domain.animal.errorcode.AnimalErrorCode
import com.team05.petmeeting.domain.animal.repository.AnimalRepository
import com.team05.petmeeting.domain.user.errorcode.UserErrorCode
import com.team05.petmeeting.domain.user.repository.UserRepository
import com.team05.petmeeting.global.exception.BusinessException
import org.springframework.stereotype.Service

@Service
class AdoptionService(
    private val adoptionApplicationRepository: AdoptionApplicationRepository,
    private val userRepository: UserRepository,
    private val animalRepository: AnimalRepository,
) {
    // 사용자별 입양 신청 목록을 조회하고 목록 응답 DTO로 변환한다.
    fun getMyAdoptions(userId: Long): List<AdoptionApplyRes> =
        adoptionApplicationRepository.findByUser_Id(userId)
            .map { it.toApplyRes() }

    // 로그인한 사용자의 입양 신청 상세를 조회하고 상세 응답 DTO로 변환한다.
    fun getApplicationDetail(userId: Long, applicationId: Long): AdoptionDetailRes {
        val application = adoptionApplicationRepository.findByIdAndUser_Id(applicationId, userId)
            .orElseThrow { BusinessException(AdoptionErrorCode.APPLICATION_NOT_FOUND) }

        return application.toDetailRes()
    }

    // 로그인한 사용자의 입양 신청을 저장하고 생성 결과를 응답 DTO로 반환한다.
    fun applyApplication(userId: Long, animalId: Long, request: AdoptionApplyReq): AdoptionApplyRes {
        if (adoptionApplicationRepository.existsByUser_IdAndAnimal_Id(userId, animalId)) {
            throw BusinessException(AdoptionErrorCode.ALREADY_APPLIED)
        }

        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(UserErrorCode.USER_NOT_FOUND) }

        val animal = animalRepository.findById(animalId)
            .orElseThrow { BusinessException(AnimalErrorCode.ANIMAL_NOT_FOUND) }

        val application = AdoptionApplication.create(
            user,
            animal,
            request.applyReason,
            request.applyTel,
        )

        val saved = adoptionApplicationRepository.save(application)
        return saved.toApplyRes()
    }

    // 로그인한 사용자의 입양 신청을 조회한 뒤 본인 신청서만 삭제한다.
    fun cancelApplication(userId: Long, applicationId: Long) {
        val application = adoptionApplicationRepository.findByIdAndUser_Id(applicationId, userId)
            .orElseThrow { BusinessException(AdoptionErrorCode.APPLICATION_NOT_FOUND) }

        adoptionApplicationRepository.delete(application)
    }
}
