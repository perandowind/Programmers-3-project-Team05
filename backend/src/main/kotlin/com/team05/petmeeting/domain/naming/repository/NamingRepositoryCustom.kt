package com.team05.petmeeting.domain.naming.repository

import com.team05.petmeeting.domain.naming.dto.NameCandidateRes
import com.team05.petmeeting.domain.naming.dto.NameCandidateRes.CandidateDto

interface NamingRepositoryCustom {
    fun getCandidates(
        animalId: Long,
        userId: Long?
    ): NameCandidateRes

    fun findAllQualifiedCandidatesByShelter(careNm: String, threshold: Int): List<CandidateDto>
}
