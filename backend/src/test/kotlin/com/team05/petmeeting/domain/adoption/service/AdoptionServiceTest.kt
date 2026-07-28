package com.team05.petmeeting.domain.adoption.service

import com.team05.petmeeting.domain.adoption.dto.AdoptionApplyReq
import com.team05.petmeeting.domain.adoption.dto.AdoptionApplyRes
import com.team05.petmeeting.domain.adoption.dto.AdoptionDetailRes
import com.team05.petmeeting.domain.adoption.entity.AdoptionApplication
import com.team05.petmeeting.domain.adoption.entity.AdoptionStatus
import com.team05.petmeeting.domain.adoption.errorcode.AdoptionErrorCode
import com.team05.petmeeting.domain.adoption.repository.AdoptionApplicationRepository
import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.animal.errorcode.AnimalErrorCode
import com.team05.petmeeting.domain.animal.repository.AnimalRepository
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.errorcode.UserErrorCode
import com.team05.petmeeting.domain.user.repository.UserRepository
import com.team05.petmeeting.global.entity.BaseEntity
import com.team05.petmeeting.global.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
// 일반 사용자가 입양 신청서를 조회, 제출, 취소하는 서비스 흐름을 검증한다.
class AdoptionServiceTest {

    @InjectMocks
    private lateinit var adoptionService: AdoptionService

    @Mock
    private lateinit var adoptionApplicationRepository: AdoptionApplicationRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var animalRepository: AnimalRepository

    @Test
    @DisplayName("로그인 사용자의 입양 신청 목록을 조회한다")
    fun getMyAdoptions() {
        val user = createUser(1L)
        val animal = createAnimal(10L, "A-001")
        val application = createApplication(100L, user, animal)
        `when`(adoptionApplicationRepository.findByUser_Id(idOf(user)))
            .thenReturn(listOf(application))

        val responses: List<AdoptionApplyRes> = adoptionService.getMyAdoptions(idOf(user))

        assertThat(responses).hasSize(1)
        assertThat(responses[0].applicationId).isEqualTo(100L)
        assertThat(responses[0].status).isEqualTo(AdoptionStatus.Processing)
        assertThat(responses[0].animalInfo.desertionNo).isEqualTo("A-001")
        assertThat(responses[0].animalInfo.careNm).isEqualTo("테스트 보호소")
    }

    @Test
    @DisplayName("로그인 사용자가 본인의 입양 신청 상세를 조회한다")
    fun getApplicationDetail() {
        val user = createUser(1L)
        val animal = createAnimal(10L, "A-001")
        val application = createApplication(100L, user, animal)
        `when`(adoptionApplicationRepository.findByIdAndUser_Id(idOf(application), idOf(user)))
            .thenReturn(java.util.Optional.of(application))

        val response: AdoptionDetailRes = adoptionService.getApplicationDetail(idOf(user), idOf(application))

        assertThat(response.applicationId).isEqualTo(100L)
        assertThat(response.status).isEqualTo(AdoptionStatus.Processing)
        assertThat(response.applyReason).isEqualTo("입양하고 싶습니다.")
        assertThat(response.applyTel).isEqualTo("010-1234-5678")
        assertThat(response.animalInfo.desertionNo).isEqualTo("A-001")
        assertThat(response.animalInfo.careTel).isEqualTo("010-0000-0000")
    }

    @Test
    @DisplayName("다른 사용자의 입양 신청 상세는 조회할 수 없다")
    fun getApplicationDetail_notOwner() {
        `when`(adoptionApplicationRepository.findByIdAndUser_Id(100L, 1L))
            .thenReturn(java.util.Optional.empty())

        assertThatThrownBy { adoptionService.getApplicationDetail(1L, 100L) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(AdoptionErrorCode.APPLICATION_NOT_FOUND)
    }

    @Test
    @DisplayName("로그인 사용자가 입양 신청서를 제출한다")
    fun applyApplication() {
        val user = createUser(1L)
        val animal = createAnimal(10L, "A-001")
        val request = createApplyRequest("가족으로 맞이하고 싶습니다.", "010-9999-8888")
        `when`(adoptionApplicationRepository.existsByUser_IdAndAnimal_Id(idOf(user), idOf(animal)))
            .thenReturn(false)
        `when`(userRepository.findById(idOf(user))).thenReturn(java.util.Optional.of(user))
        `when`(animalRepository.findById(idOf(animal))).thenReturn(java.util.Optional.of(animal))
        `when`(adoptionApplicationRepository.save(any(AdoptionApplication::class.java)))
            .thenAnswer { invocation ->
                val saved = invocation.getArgument<AdoptionApplication>(0)
                setId(saved, 100L)
                saved
            }

        val response: AdoptionApplyRes = adoptionService.applyApplication(idOf(user), idOf(animal), request)

        assertThat(response.applicationId).isEqualTo(100L)
        assertThat(response.status).isEqualTo(AdoptionStatus.Processing)
        assertThat(response.animalInfo.desertionNo).isEqualTo("A-001")
        verify(adoptionApplicationRepository).save(any(AdoptionApplication::class.java))
    }

    @Test
    @DisplayName("이미 신청한 동물은 다시 신청할 수 없다")
    fun applyApplication_alreadyApplied() {
        `when`(adoptionApplicationRepository.existsByUser_IdAndAnimal_Id(1L, 10L))
            .thenReturn(true)

        assertThatThrownBy {
            adoptionService.applyApplication(
                1L,
                10L,
                createApplyRequest("입양하고 싶습니다.", "010-1234-5678"),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(AdoptionErrorCode.ALREADY_APPLIED)
        verify(adoptionApplicationRepository, never()).save(any())
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 입양 신청할 수 없다")
    fun applyApplication_userNotFound() {
        `when`(adoptionApplicationRepository.existsByUser_IdAndAnimal_Id(1L, 10L))
            .thenReturn(false)
        `when`(userRepository.findById(1L)).thenReturn(java.util.Optional.empty())

        assertThatThrownBy {
            adoptionService.applyApplication(
                1L,
                10L,
                createApplyRequest("입양하고 싶습니다.", "010-1234-5678"),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(UserErrorCode.USER_NOT_FOUND)
    }

    @Test
    @DisplayName("존재하지 않는 동물에는 입양 신청할 수 없다")
    fun applyApplication_animalNotFound() {
        val user = createUser(1L)
        `when`(adoptionApplicationRepository.existsByUser_IdAndAnimal_Id(1L, 10L))
            .thenReturn(false)
        `when`(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user))
        `when`(animalRepository.findById(10L)).thenReturn(java.util.Optional.empty())

        assertThatThrownBy {
            adoptionService.applyApplication(
                1L,
                10L,
                createApplyRequest("입양하고 싶습니다.", "010-1234-5678"),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(AnimalErrorCode.ANIMAL_NOT_FOUND)
    }

    @Test
    @DisplayName("로그인 사용자가 본인의 입양 신청을 취소한다")
    fun cancelApplication() {
        val user = createUser(1L)
        val application = createApplication(100L, user, createAnimal(10L, "A-001"))
        `when`(adoptionApplicationRepository.findByIdAndUser_Id(idOf(application), idOf(user)))
            .thenReturn(java.util.Optional.of(application))

        adoptionService.cancelApplication(idOf(user), idOf(application))

        verify(adoptionApplicationRepository).delete(application)
    }

    @Test
    @DisplayName("다른 사용자의 입양 신청은 취소할 수 없다")
    fun cancelApplication_notOwner() {
        `when`(adoptionApplicationRepository.findByIdAndUser_Id(100L, 1L))
            .thenReturn(java.util.Optional.empty())

        assertThatThrownBy { adoptionService.cancelApplication(1L, 100L) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(AdoptionErrorCode.APPLICATION_NOT_FOUND)
    }

    private fun createUser(id: Long): User {
        val user = User.create("user@test.com", "사용자", "홍길동")
        setId(user, id)
        return user
    }

    private fun createAnimal(id: Long, desertionNo: String): Animal {
        val animal = Animal.builder()
            .desertionNo(desertionNo)
            .processState("보호중")
            .stateGroup(0)
            .kindFullNm("믹스견")
            .specialMark("특이사항 없음")
            .careNm("테스트 보호소")
            .careOwnerNm("보호소장")
            .careTel("010-0000-0000")
            .careAddr("서울시")
            .totalCheerCount(0)
            .build()
        setId(animal, id)
        return animal
    }

    private fun createApplication(id: Long, user: User, animal: Animal): AdoptionApplication {
        val application = AdoptionApplication.create(
            user,
            animal,
            "입양하고 싶습니다.",
            "010-1234-5678",
        )
        setId(application, id)
        return application
    }

    private fun createApplyRequest(applyReason: String, applyTel: String): AdoptionApplyReq =
        AdoptionApplyReq(applyReason, applyTel)

    private fun setId(entity: BaseEntity, id: Long) {
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }

    private fun idOf(entity: BaseEntity): Long = requireNotNull(entity.id)
}
