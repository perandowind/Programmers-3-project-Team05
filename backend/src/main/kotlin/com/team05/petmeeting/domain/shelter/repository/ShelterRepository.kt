package com.team05.petmeeting.domain.shelter.repository

import com.team05.petmeeting.domain.shelter.entity.Shelter
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param


@Repository
interface ShelterRepository : JpaRepository<Shelter, String> {
    fun findByCareRegNoIn(ids: Set<String>): List<Shelter>


    @Query(
        """
        SELECT s
        FROM Shelter s
        WHERE s.careNm LIKE CONCAT('%', :keyword, '%')
           OR s.careAddr LIKE CONCAT('%', :keyword, '%')
           OR s.orgNm LIKE CONCAT('%', :keyword, '%')
           OR s.careRegNo LIKE CONCAT('%', :keyword, '%')
        """
    )
    fun searchShelters(
        @Param("keyword") keyword: String,
        pageable: Pageable
    ): Page<Shelter>
}
