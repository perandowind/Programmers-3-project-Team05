package com.team05.petmeeting.domain.naming.service

import com.team05.petmeeting.domain.animal.dto.external.AnimalItem
import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.animal.entity.Animal.Companion.from
import com.team05.petmeeting.domain.animal.repository.AnimalRepository
import com.team05.petmeeting.domain.naming.entity.AnimalNameCandidate
import com.team05.petmeeting.domain.naming.entity.NameVoteHistory
import com.team05.petmeeting.domain.naming.errorcode.NamingErrorCode
import com.team05.petmeeting.domain.naming.repository.AnimalNameCandidateRepository
import com.team05.petmeeting.domain.naming.repository.NameVoteHistoryRepository
import com.team05.petmeeting.domain.shelter.dto.ShelterCommand
import com.team05.petmeeting.domain.shelter.entity.Shelter.Companion.create
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.repository.UserRepository
import com.team05.petmeeting.global.exception.BusinessException
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.function.Executable
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class NamingServiceTest {
    @InjectMocks
    lateinit var namingService: NamingService

    @Mock
    lateinit var animalRepository: AnimalRepository

    @Mock
    lateinit var candidateRepository: AnimalNameCandidateRepository

    @Mock
    lateinit var voteHistoryRepository: NameVoteHistoryRepository

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var badWordService: BadWordService

    lateinit var user: User
    lateinit var animal: Animal

    @BeforeEach
    fun setUp() {
        // 테스트용 기본 객체 생성 (Reflection을 사용해 ID 강제 주입)
        user = User.create("test@test.com", "테스터", "홍길동")
        ReflectionTestUtils.setField(user, "id", 1L)

        // Animal 객체 생성 및 상태 설정
        animal = from(AnimalItem()) // 필요한 필드만 채워진 item 가정
        ReflectionTestUtils.setField(animal, "id", 100L)
        ReflectionTestUtils.setField(animal, "stateGroup", 0) // 보호중
    }

    @Test
    @DisplayName("이름 제안 성공 - 금칙어가 없고 신규 이름일 경우")
    fun proposeName_Success() {
        // given
        val proposedName = "초코"
        BDDMockito.given(animalRepository.findById(100L))
            .willReturn(Optional.of(animal))
        BDDMockito.given(badWordService.isBadWord(proposedName)).willReturn(false)
        BDDMockito.given<AnimalNameCandidate?>(candidateRepository.findByAnimalIdAndProposedName(100L, proposedName))
            .willReturn(null)

        // 이 부분이 핵심입니다: vote() 메서드 내부에서 호출되는 userRepository 조회 대응
        BDDMockito.given(userRepository.findById(1L)).willReturn(Optional.of<User>(user))

        val newCandidate = AnimalNameCandidate(animal, user, proposedName)
        ReflectionTestUtils.setField(newCandidate, "id", 1L)

        // candidateId를 통해 다시 후보를 찾는 로직 대응 (vote 메서드 내부)
        BDDMockito.given(candidateRepository.findById(1L))
            .willReturn(Optional.of(newCandidate))
        BDDMockito.given(
            candidateRepository.save(
                ArgumentMatchers.any(
                    AnimalNameCandidate::class.java
                )
            )
        ).willReturn(newCandidate)

        // when
        val response = namingService.proposeName(100L, 1L, proposedName)

        // then
        Assertions.assertThat(response.proposedName).isEqualTo(proposedName)
        Mockito.verify(candidateRepository, Mockito.times(1))
            .save(ArgumentMatchers.any(AnimalNameCandidate::class.java))
        Mockito.verify(voteHistoryRepository, Mockito.times(1))
            .save(ArgumentMatchers.any(NameVoteHistory::class.java))
    }

    @Test
    @DisplayName("중복 투표 방지 - 이미 투표한 유저가 다시 투표 시도 시 예외 발생")
    fun vote_Fail_AlreadyVoted() {
        // given
        val candidateId = 1L
        val candidate = AnimalNameCandidate(animal, user, "바둑이")
        BDDMockito.given(candidateRepository.findById(candidateId)).willReturn(
            Optional.of(candidate)
        )
        BDDMockito.given(userRepository.findById(1L)).willReturn(Optional.of(user))

        // 이미 투표 이력이 있다고 가정
        BDDMockito.given(voteHistoryRepository.existsByUserIdAndAnimalId(1L, 100L)).willReturn(true)

        // when & then
        val exception =
            org.junit.jupiter.api.Assertions.assertThrows<BusinessException>(BusinessException::class.java, Executable {
                namingService.vote(candidateId, 1L)
            })

        Assertions.assertThat(exception.errorCode).isEqualTo(NamingErrorCode.ALREADY_VOTED)
    }

    @Test
    @DisplayName("관리자 이름 확정 - Animal 엔티티에 이름이 정상 반영되는지 확인")
    fun confirmName_Success() {
        // 1. 공통 Shelter 생성
        val commonCareRegNo = "123456789"

        // ShelterCommand를 mock하거나 직접 생성하여 Shelter 객체 생성
        val shelter = create(
            ShelterCommand(
                commonCareRegNo, "행복보호소", "010-1234-5678",
                "서울", "관리자", "서울시", LocalDateTime.now()
            )
        )

        // 2. Animal에 Shelter 주입 (Animal 엔티티에 getShelter()가 있다고 가정)
        // Animal 생성 시점에 Shelter가 들어가는 구조라면 생성 시 주입,
        // 아니라면 ReflectionTestUtils 사용
        ReflectionTestUtils.setField(animal, "shelter", shelter)

        // 3. Manager(User) 생성 및 Shelter 주입
        val manager = User.create("admin@test.com", "관리자", "김관리")
        ReflectionTestUtils.setField(manager, "id", 999L)
        // User 엔티티에 shelter 필드가 있고, assignShelter 같은 메서드가 있다면 호출
        // 없다면 Reflection 사용
        ReflectionTestUtils.setField(manager, "shelter", shelter)

        // 4. Given 설정
        val candidateId = 1L
        val candidate = AnimalNameCandidate(animal, user, "복실이")

        BDDMockito.given(candidateRepository.findById(candidateId)).willReturn(
            Optional.of(candidate)
        )
        BDDMockito.given(userRepository.findById(999L)).willReturn(Optional.of(manager))

        // 5. When
        namingService.confirmName(candidateId, 999L)

        // 6. Then
        Assertions.assertThat(candidate.isConfirmed).isTrue()
        Assertions.assertThat(animal.name).isEqualTo("복실이")
    }
}
