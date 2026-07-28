package com.team05.petmeeting.domain.adoption.service

import com.team05.petmeeting.domain.adoption.dto.AdoptionApplyRes
import com.team05.petmeeting.domain.adoption.dto.AdoptionDetailRes
import com.team05.petmeeting.domain.adoption.dto.AdoptionReviewReq
import com.team05.petmeeting.domain.adoption.entity.AdoptionApplication
import com.team05.petmeeting.domain.adoption.entity.AdoptionStatus
import com.team05.petmeeting.domain.adoption.errorcode.AdoptionErrorCode
import com.team05.petmeeting.domain.adoption.repository.AdoptionApplicationRepository
import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.shelter.dto.ShelterCommand
import com.team05.petmeeting.domain.shelter.entity.Shelter
import com.team05.petmeeting.domain.shelter.repository.ShelterRepository
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.global.entity.BaseEntity
import com.team05.petmeeting.global.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class AdoptionAdminServiceTest {

    @InjectMocks
    private lateinit var adoptionAdminService: AdoptionAdminService

    @Mock
    private lateinit var adoptionApplicationRepository: AdoptionApplicationRepository

    @Mock
    private lateinit var shelterRepository: ShelterRepository

    @Test
    @DisplayName("담당 보호소의 입양 신청 목록만 조회한다")
    fun getManagedShelterApplications() {
        val manager = createUser(1L, "manager@test.com")
        val otherManager = createUser(2L, "other-manager@test.com")
        val applicant = createUser(3L, "applicant@test.com")

        val managedApplication = createApplication(
            1L,
            applicant,
            createAnimal("A-001", "담당보호소", createShelter("S-001", "담당보호소", manager)),
        )
        val otherApplication = createApplication(
            2L,
            applicant,
            createAnimal("A-002", "다른보호소", createShelter("S-002", "다른보호소", otherManager)),
        )
        `when`(shelterRepository.findById("S-001"))
            .thenReturn(java.util.Optional.of(requireNotNull(managedApplication.animal.shelter)))
        `when`(adoptionApplicationRepository.findByAnimal_Shelter_CareRegNo("S-001"))
            .thenReturn(listOf(managedApplication))

        val responses: List<AdoptionApplyRes> =
            adoptionAdminService.getManagedShelterApplications(idOf(manager), "S-001")

        assertThat(responses).hasSize(1)
        assertThat(responses[0].applicationId).isEqualTo(managedApplication.id)
        assertThat(responses[0].status).isEqualTo(AdoptionStatus.Processing)
        assertThat(responses[0].animalInfo.careNm).isEqualTo("담당보호소")
        assertThat(otherApplication.id).isEqualTo(2L)
    }

    @Test
    @DisplayName("담당 보호소의 입양 신청 상세를 조회한다")
    fun getManagedShelterApplicationDetail() {
        val manager = createUser(1L, "manager@test.com")
        val applicant = createUser(2L, "applicant@test.com")
        val animal = createAnimal("A-001", "담당보호소", createShelter("S-001", "담당보호소", manager))
        val application = createApplication(1L, applicant, animal)
        `when`(shelterRepository.findById("S-001"))
            .thenReturn(java.util.Optional.of(requireNotNull(animal.shelter)))
        `when`(adoptionApplicationRepository.findById(idOf(application)))
            .thenReturn(java.util.Optional.of(application))

        val response: AdoptionDetailRes =
            adoptionAdminService.getManagedShelterApplicationDetail(idOf(manager), "S-001", idOf(application))

        assertThat(response.applicationId).isEqualTo(application.id)
        assertThat(response.status).isEqualTo(AdoptionStatus.Processing)
        assertThat(response.applyReason).isEqualTo("입양하고 싶습니다.")
        assertThat(response.applyTel).isEqualTo("010-1234-5678")
        assertThat(response.animalInfo.desertionNo).isEqualTo("A-001")
        assertThat(response.animalInfo.careNm).isEqualTo("담당보호소")
    }

    @Test
    @DisplayName("다른 보호소의 입양 신청 상세는 조회할 수 없다")
    fun getManagedShelterApplicationDetail_otherShelter() {
        val manager = createUser(1L, "manager@test.com")
        val otherManager = createUser(2L, "other-manager@test.com")
        val applicant = createUser(3L, "applicant@test.com")
        val managedShelter = createShelter("S-001", "담당보호소", manager)
        val animal = createAnimal("A-001", "다른보호소", createShelter("S-002", "다른보호소", otherManager))
        val application = createApplication(1L, applicant, animal)
        `when`(shelterRepository.findById("S-001"))
            .thenReturn(java.util.Optional.of(managedShelter))
        `when`(adoptionApplicationRepository.findById(idOf(application)))
            .thenReturn(java.util.Optional.of(application))

        assertThatThrownBy {
            adoptionAdminService.getManagedShelterApplicationDetail(idOf(manager), "S-001", idOf(application))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(AdoptionErrorCode.FORBIDDEN_SHELTER_APPLICATION)
    }

    @Test
    @DisplayName("해당 careRegNo 보호소 관리자가 아니면 조회할 수 없다")
    fun getManagedShelterApplicationDetail_notShelterManager() {
        val manager = createUser(1L, "manager@test.com")
        val otherManager = createUser(2L, "other-manager@test.com")
        val otherShelter = createShelter("S-001", "다른보호소", otherManager)
        `when`(shelterRepository.findById("S-001"))
            .thenReturn(java.util.Optional.of(otherShelter))

        assertThatThrownBy {
            adoptionAdminService.getManagedShelterApplicationDetail(idOf(manager), "S-001", 1L)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(AdoptionErrorCode.UNAUTHORIZED_SHELTER)
    }

    @Test
    @DisplayName("담당 보호소의 입양 신청을 승인한다")
    fun reviewApplication_approve() {
        val manager = createUser(1L, "manager@test.com")
        val applicant = createUser(2L, "applicant@test.com")
        val animal = createAnimal("A-001", "담당보호소", createShelter("S-001", "담당보호소", manager))
        val application = createApplication(1L, applicant, animal)
        `when`(shelterRepository.findById("S-001"))
            .thenReturn(java.util.Optional.of(requireNotNull(animal.shelter)))
        `when`(adoptionApplicationRepository.findById(idOf(application)))
            .thenReturn(java.util.Optional.of(application))

        val response = adoptionAdminService.reviewApplication(
            idOf(manager),
            "S-001",
            idOf(application),
            AdoptionReviewReq(AdoptionStatus.Approved, null),
        )

        assertThat(application.status).isEqualTo(AdoptionStatus.Approved)
        assertThat(application.reviewedAt).isNotNull()
        assertThat(application.rejectionReason).isNull()
        assertThat(response.status).isEqualTo(AdoptionStatus.Approved)
    }

    @Test
    @DisplayName("담당 보호소의 입양 신청을 거절한다")
    fun reviewApplication_reject() {
        val manager = createUser(1L, "manager@test.com")
        val applicant = createUser(2L, "applicant@test.com")
        val animal = createAnimal("A-001", "담당보호소", createShelter("S-001", "담당보호소", manager))
        val application = createApplication(1L, applicant, animal)
        `when`(shelterRepository.findById("S-001"))
            .thenReturn(java.util.Optional.of(requireNotNull(animal.shelter)))
        `when`(adoptionApplicationRepository.findById(idOf(application)))
            .thenReturn(java.util.Optional.of(application))

        val response = adoptionAdminService.reviewApplication(
            idOf(manager),
            "S-001",
            idOf(application),
            AdoptionReviewReq(AdoptionStatus.Rejected, "조건이 맞지 않습니다."),
        )

        assertThat(application.status).isEqualTo(AdoptionStatus.Rejected)
        assertThat(application.reviewedAt).isNotNull()
        assertThat(application.rejectionReason).isEqualTo("조건이 맞지 않습니다.")
        assertThat(response.status).isEqualTo(AdoptionStatus.Rejected)
        assertThat(response.rejectionReason).isEqualTo("조건이 맞지 않습니다.")
    }

    @Test
    @DisplayName("담당 보호소의 입양 신청을 검토중으로 변경한다")
    fun reviewApplication_markProcessing() {
        val manager = createUser(1L, "manager@test.com")
        val applicant = createUser(2L, "applicant@test.com")
        val animal = createAnimal("A-001", "담당보호소", createShelter("S-001", "담당보호소", manager))
        val application = createApplication(1L, applicant, animal)
        application.reject("이전 거절 사유")
        `when`(shelterRepository.findById("S-001"))
            .thenReturn(java.util.Optional.of(requireNotNull(animal.shelter)))
        `when`(adoptionApplicationRepository.findById(idOf(application)))
            .thenReturn(java.util.Optional.of(application))

        val response = adoptionAdminService.reviewApplication(
            idOf(manager),
            "S-001",
            idOf(application),
            AdoptionReviewReq(AdoptionStatus.Processing, null),
        )

        assertThat(application.status).isEqualTo(AdoptionStatus.Processing)
        assertThat(application.reviewedAt).isNull()
        assertThat(application.rejectionReason).isNull()
        assertThat(response.status).isEqualTo(AdoptionStatus.Processing)
        assertThat(response.reviewedAt).isNull()
        assertThat(response.rejectionReason).isNull()
    }

    @Test
    @DisplayName("거절 사유 없이 거절할 수 없다")
    fun reviewApplication_rejectWithoutReason() {
        val manager = createUser(1L, "manager@test.com")
        val applicant = createUser(2L, "applicant@test.com")
        val animal = createAnimal("A-001", "담당보호소", createShelter("S-001", "담당보호소", manager))
        val application = createApplication(1L, applicant, animal)
        `when`(shelterRepository.findById("S-001"))
            .thenReturn(java.util.Optional.of(requireNotNull(animal.shelter)))
        `when`(adoptionApplicationRepository.findById(idOf(application)))
            .thenReturn(java.util.Optional.of(application))

        assertThatThrownBy {
            adoptionAdminService.reviewApplication(
                idOf(manager),
                "S-001",
                idOf(application),
                AdoptionReviewReq(AdoptionStatus.Rejected, " "),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(AdoptionErrorCode.REJECTION_REASON_REQUIRED)
    }

    private fun createUser(id: Long, email: String): User {
        val user = User.create(email, email, "홍길동")
        setId(user, id)
        return user
    }

    private fun createShelter(careRegNo: String, careNm: String, manager: User): Shelter {
        val shelter = Shelter.create(
            ShelterCommand(
                careRegNo,
                careNm,
                "010-0000-0000",
                "서울시",
                "보호소장",
                "서울시",
                LocalDateTime.now(),
            ),
        )
        shelter.assignUser(manager)
        return shelter
    }

    private fun createAnimal(desertionNo: String, careNm: String, shelter: Shelter): Animal {
        val animal = Animal.builder()
            .desertionNo(desertionNo)
            .processState("보호중")
            .stateGroup(0)
            .kindFullNm("믹스견")
            .specialMark("특이사항 없음")
            .careNm(careNm)
            .careOwnerNm("보호소장")
            .careTel("010-0000-0000")
            .careAddr("서울시")
            .totalCheerCount(0)
            .build()
        animal.assignShelter(shelter)
        return animal
    }

    private fun createApplication(id: Long, applicant: User, animal: Animal): AdoptionApplication {
        val application = AdoptionApplication.create(
            applicant,
            animal,
            "입양하고 싶습니다.",
            "010-1234-5678",
        )
        setId(application, id)
        return application
    }

    private fun setId(entity: BaseEntity, id: Long) {
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }

    private fun idOf(entity: BaseEntity): Long = requireNotNull(entity.id)
}
