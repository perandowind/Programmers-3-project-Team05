package com.team05.petmeeting.domain.naming.dto

// 후보 목록 반환 DTO
data class NameCandidateRes(
    val animalId: Long,
    val animalName: String?,
    val candidateDtoList: List<CandidateDto>,
    val totalCandidates: Int
) {
    data class CandidateDto(
        val candidateId: Long,
        val animalId: Long,  // 추가됨: 어떤 동물의 후보인지 식별
        val proposedName: String,
        val proposerNickname: String,
        val voteCount: Int,
        val isVoted: Boolean // 현재 로그인한 유저가 투표했는지 여부
    )
}
