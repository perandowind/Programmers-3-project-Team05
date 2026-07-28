package com.team05.petmeeting.domain.naming.service

import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.domain.animal.errorcode.AnimalErrorCode
import com.team05.petmeeting.domain.animal.repository.AnimalRepository
import com.team05.petmeeting.domain.naming.dto.BadWordAddRes
import com.team05.petmeeting.domain.naming.dto.BadWordListRes
import com.team05.petmeeting.domain.naming.dto.BadWordListRes.BadWordDto
import com.team05.petmeeting.domain.naming.dto.NameCandidateRes
import com.team05.petmeeting.domain.naming.dto.NameProposalRes
import com.team05.petmeeting.domain.naming.entity.AnimalNameCandidate
import com.team05.petmeeting.domain.naming.entity.BadWord
import com.team05.petmeeting.domain.naming.entity.NameVoteHistory
import com.team05.petmeeting.domain.naming.entity.QBadWord.badWord
import com.team05.petmeeting.domain.naming.errorcode.NamingErrorCode
import com.team05.petmeeting.domain.naming.repository.AnimalNameCandidateRepository
import com.team05.petmeeting.domain.naming.repository.NameVoteHistoryRepository
import com.team05.petmeeting.domain.user.errorcode.UserErrorCode
import com.team05.petmeeting.domain.user.repository.UserRepository
import com.team05.petmeeting.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class NamingService(
    private val candidateRepository: AnimalNameCandidateRepository,
    private val voteHistoryRepository: NameVoteHistoryRepository,
    private val badWordService: BadWordService,// Redis 기반 금칙어 검증 서비스
    private val userRepository: UserRepository,
    private val animalRepository: AnimalRepository,
) {

    companion object{
        const val QUALIFIED_THRESHOLD = 10 // 자격 임계 득표수 10표
    }

    @Transactional(readOnly = true)
    fun getCandidates(animalId: Long, userId: Long?): NameCandidateRes {
        return candidateRepository.getCandidates(animalId, userId)
    }

    fun proposeName(animalId: Long, userId: Long, proposedName: String): NameProposalRes {
        // 동물 존재 여부
        val animal = animalRepository.findById(animalId)
            .orElseThrow{ BusinessException(AnimalErrorCode.ANIMAL_NOT_FOUND) }

        // 이미 종료된 동물인지 검증
        validateAnimalStatus(animal)

        // 금칙어 검증
        if (badWordService.isBadWord(proposedName)) {
            throw BusinessException(NamingErrorCode.BAD_WORD_INCLUDED)
        }

        // 해당 동물의 이름 후보 중 동일한 이름이 있는지 확인
        candidateRepository
            .findByAnimalIdAndProposedName(animalId, proposedName)
            ?.let { candidate ->
                val candidateId = requireNotNull(candidate.id)
                vote(candidateId, userId) // 내부 투표 로직 호출
                return NameProposalRes(candidateId, proposedName)
            }

        val proposer = userRepository.findById(userId)
            .orElseThrow{ BusinessException(UserErrorCode.USER_NOT_FOUND) }

        val newCandidate = AnimalNameCandidate(animal, proposer, proposedName)
        val savedCandidate = candidateRepository.save<AnimalNameCandidate>(newCandidate)
        val savedCandidateId = requireNotNull(savedCandidate.id)

        // 4. 제안자 본인의 첫 투표 처리
        vote(savedCandidateId, userId)
        return NameProposalRes(savedCandidateId, proposedName)
    }

    fun vote(candidateId: Long, userId: Long) {
        val candidate = candidateRepository.findById(candidateId)
            .orElseThrow{ BusinessException(NamingErrorCode.CANDIDATE_NOT_FOUND) }

        // 투표하려는 후보가 속한 동물이 여전히 '보호중'인지 체크
        validateAnimalStatus(candidate.animal)

        val user = userRepository.findById(userId)
            .orElseThrow{ BusinessException(UserErrorCode.USER_NOT_FOUND) }

        // 중복 투표 방지 -> 한 유저는 한 동물당 한번만 투표 가능
        if (voteHistoryRepository.existsByUserIdAndAnimalId(userId, requireNotNull(candidate.animal.id))) {
            throw BusinessException(NamingErrorCode.ALREADY_VOTED)
        }

        // 투표 이력 저장
        voteHistoryRepository.save(NameVoteHistory(user, candidate.animal, candidate))

        // 후보 테이블의 투표수 증가
        candidate.addVoteCount()
    }

    @Transactional(readOnly = true)
    fun getAdminQualifiedList(managerId: Long): List<NameCandidateRes> {
        val manager = userRepository.findById(managerId)
            .orElseThrow{ BusinessException(UserErrorCode.USER_NOT_FOUND) }

        val careNm = manager.shelter?.careNm ?: throw BusinessException(NamingErrorCode.ACCESS_DENIED)

        // 1. 해당 보호소의 10표 이상 모든 후보 조회
        val allQualified = candidateRepository.findAllQualifiedCandidatesByShelter(careNm, QUALIFIED_THRESHOLD)

        // 2. 동물(AnimalId)별로 그룹화하여 "가장 득표수가 높은 후보 1개"만 추출
        // (쿼리에서 voteCount.desc()로 정렬했으므로 첫 번째 값만 취하면 됨)
        // Todo: 코틀린 코드 개선 작성
        return allQualified
            .distinctBy { it.animalId } // 첫 번째(득표수 높은) 데이터만 남김 = 이후 중복값 버림
            .map { dto ->
                NameCandidateRes(dto.animalId, null, listOf(dto), 1)
            }
    }

    fun confirmName(candidateId: Long, managerId: Long) {
        val candidate = candidateRepository.findById(candidateId)
            .orElseThrow{ BusinessException(NamingErrorCode.CANDIDATE_NOT_FOUND) }

        // 상태 검증
        validateAnimalStatus(candidate.animal)

        // 3. 권한 검증 (팀원 구현부 연동 대비)
        val manager = userRepository.findById(managerId)
            .orElseThrow{ BusinessException(UserErrorCode.USER_NOT_FOUND) }

        val managerShelter = manager.shelter
        val animalShelter = candidate.animal.shelter

        // 관리자의 careRegNo 과 동물이 속한 보호소의 careRegNo 을 비교
        if (managerShelter == null || managerShelter.careNm != animalShelter?.careNm) {
            throw BusinessException(NamingErrorCode.ACCESS_DENIED)
        }

        // 이름 확정 처리
        candidate.confirmName() // isConfirmed = true
        candidate.animal.updateName(candidate.proposedName) // 동물 엔터티 이름 반영
    }

    // 금칙어 관리 (BadWordService를 거쳐 DB와 Redis 동시 처리)
    fun getBadWords(): BadWordListRes {
        val words = badWordService.findAll().map { bw ->
            BadWordDto(requireNotNull(bw.id), bw.word, requireNotNull(bw.createdAt).toString())
        }
        return BadWordListRes(words, words.size)
    }

    @Transactional
    fun addBadWord(word: String): BadWordAddRes {
        // BadWordService 내부에서 DB 저장 + Redis 추가를 모두 처리하고 저장된 엔티티 반환
        val savedBadWord = badWordService.register(word)
        return BadWordAddRes(
            requireNotNull(savedBadWord.id),
            savedBadWord.word,
            requireNotNull(savedBadWord.createdAt)
        )
    }

    @Transactional
    fun deleteBadWord(badwordId: Long) {
        // BadWordService 내부에서 존재 여부 확인 + DB 삭제 + Redis 제거를 모두 처리
        badWordService.remove(badwordId)
    }

    private fun validateAnimalStatus(animal: Animal) {
        // 상태가 '보호중 == 1'인 경우에만 가능
        if (animal.stateGroup == 1) {
            throw BusinessException(NamingErrorCode.ALREADY_COMPLETED_ANIMAL)
        }
        // 이미 이름이 확정되었는지 체크
        if (!animal.name.isNullOrBlank()) {
            throw BusinessException(NamingErrorCode.ALREADY_HAS_NAME)
        }
    }
}
