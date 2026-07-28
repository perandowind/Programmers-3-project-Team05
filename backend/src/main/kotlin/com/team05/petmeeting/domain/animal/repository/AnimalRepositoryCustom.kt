package com.team05.petmeeting.domain.animal.repository

import com.querydsl.core.Tuple
import com.team05.petmeeting.domain.animal.entity.Animal
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AnimalRepositoryCustom {
    fun findAnimalsWithFilter(
        region: String?,
        kind: String?,
        kindFullNm: String?,
        stateGroup: Int?,
        pageable: Pageable
    ): Page<Animal>

    fun findDistinctKindFullNames(): List<Tuple>

    fun findMatched(
        species: String,
        size: String,
        region: String,
        housing: String,
        activity: String,
        experience: String,
        pageable: Pageable
    ): Page<Animal>
}
