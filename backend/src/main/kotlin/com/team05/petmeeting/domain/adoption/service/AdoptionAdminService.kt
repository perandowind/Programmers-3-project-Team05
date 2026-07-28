package com.team05.petmeeting.domain.adoption.service

import com.team05.petmeeting.domain.adoption.dto.AdoptionApplyRes
import com.team05.petmeeting.domain.adoption.dto.AdoptionDetailRes
import com.team05.petmeeting.domain.adoption.dto.AdoptionReviewReq
import com.team05.petmeeting.domain.adoption.dto.toApplyRes
import com.team05.petmeeting.domain.adoption.dto.toDetailRes
import com.team05.petmeeting.domain.adoption.entity.AdoptionApplication
import com.team05.petmeeting.domain.adoption.entity.AdoptionStatus
import com.team05.petmeeting.domain.adoption.errorcode.AdoptionErrorCode
import com.team05.petmeeting.domain.adoption.repository.AdoptionApplicationRepository
import com.team05.petmeeting.domain.shelter.entity.Shelter
import com.team05.petmeeting.domain.shelter.errorcode.ShelterErrorCode
import com.team05.petmeeting.domain.shelter.repository.ShelterRepository
import com.team05.petmeeting.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdoptionAdminService(
    private val adoptionApplicationRepository: AdoptionApplicationRepository,
    private val shelterRepository: ShelterRepository,
) {
    // careRegNo 보호소의 관리자인지 확인한 뒤 해당 보호소의 입양 신청 목록만 반환한다.
    @Transactional(readOnly = true)
    fun getManagedShelterApplications(userId: Long, careRegNo: String): List<AdoptionApplyRes> {
        validateShelterManager(userId, careRegNo)

        return adoptionApplicationRepository.findByAnimal_Shelter_CareRegNo(careRegNo)
            .map { it.toApplyRes() }
    }

    // careRegNo 보호소의 관리자인지 확인한 뒤 담당 보호소 신청 상세만 반환한다.
    @Transactional(readOnly = true)
    fun getManagedShelterApplicationDetail(
        userId: Long,
        careRegNo: String,
        applicationId: Long,
    ): AdoptionDetailRes {
        validateShelterManager(userId, careRegNo)

        val application = getShelterApplication(careRegNo, applicationId)
        return application.toDetailRes()
    }

    // careRegNo 보호소 관리자가 입양 신청 상태를 승인/거절/검토중으로 변경한다.
    @Transactional
    fun reviewApplication(
        userId: Long,
        careRegNo: String,
        applicationId: Long,
        request: AdoptionReviewReq,
    ): AdoptionDetailRes {
        validateShelterManager(userId, careRegNo)
        val application = getShelterApplication(careRegNo, applicationId)

        when (request.status) {
            AdoptionStatus.Approved -> application.approve()
            AdoptionStatus.Rejected -> rejectApplication(application, request.rejectionReason)
            AdoptionStatus.Processing -> application.markProcessing()
        }

        return application.toDetailRes()
    }

    private fun rejectApplication(application: AdoptionApplication, rejectionReason: String?) {
        if (rejectionReason.isNullOrBlank()) {
            throw BusinessException(AdoptionErrorCode.REJECTION_REASON_REQUIRED)
        }

        application.reject(rejectionReason)
    }

    private fun getShelterApplication(careRegNo: String, applicationId: Long): AdoptionApplication {
        val application = adoptionApplicationRepository.findById(applicationId)
            .orElseThrow { BusinessException(AdoptionErrorCode.APPLICATION_NOT_FOUND) }

        if (!isShelterApplication(application, careRegNo)) {
            throw BusinessException(AdoptionErrorCode.FORBIDDEN_SHELTER_APPLICATION)
        }

        return application
    }

    // 사용자가 careRegNo 보호소의 관리자인지 확인한다.
    private fun validateShelterManager(userId: Long, careRegNo: String) {
        val shelter = shelterRepository.findById(careRegNo)
            .orElseThrow { BusinessException(ShelterErrorCode.SHELTER_NOT_FOUND) }

        if (!shelter.isManagedBy(userId)) {
            throw BusinessException(AdoptionErrorCode.UNAUTHORIZED_SHELTER)
        }
    }

    // 신청 동물의 보호소가 요청한 careRegNo 보호소인지 확인한다.
    private fun isShelterApplication(application: AdoptionApplication, careRegNo: String): Boolean {
        val shelter: Shelter? = application.animal.shelter
        return shelter != null && shelter.careRegNo == careRegNo
    }
}
