package com.team05.petmeeting.domain.shelter.service

import com.team05.petmeeting.domain.shelter.dto.ShelterCommand
import com.team05.petmeeting.domain.shelter.dto.ShelterListRes
import com.team05.petmeeting.domain.shelter.dto.ShelterRes
import com.team05.petmeeting.domain.shelter.entity.Shelter
import com.team05.petmeeting.domain.shelter.entity.Shelter.Companion.create
import com.team05.petmeeting.domain.shelter.errorcode.ShelterErrorCode
import com.team05.petmeeting.domain.shelter.repository.ShelterRepository
import com.team05.petmeeting.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.Pageable

@Service
@Transactional
class ShelterService(private val shelterRepository: ShelterRepository) {
    /*
        * 외부 API 데이터 예시
        * "careRegNo" : "343447202600001",
        * "careNm" : "음성군 동물보호센터",
        * "careTel" : "043-877-3081",
        * "careAddr" : "충청북도 음성군 삼성면 대금로 715-5",
        * "careOwnerNm" : "음성군수",
        * "orgNm" : "충청북도 음성군",
        * "updTm" : "2026-04-15 14:19:49.0"
        * updTm 비교해서 갱신 필요하면 보호소 정보도 갱신
        * cmd 1개 -> DB 조회 여러번
        */
    fun createOrUpdateShelter(cmd: ShelterCommand): Shelter {
        return shelterRepository.findById(cmd.careRegNo)
            .map { existing ->
                if (existing.updTm.isBefore(cmd.updTm)) {
                    existing.updateFrom(cmd)
                }
                existing
            }
            .orElseGet {
                shelterRepository.save(create(cmd))
            }
    }

    /*
     * n개 cmd -> DB 조회 한번
     */
    fun createOrUpdateShelters(cmds: List<ShelterCommand>) {
        val ids = cmds.map { it.careRegNo }.toSet()

        // 모든 careRegNo 한번에 다 조회해서 map에 저장
        val map = shelterRepository.findByCareRegNoIn(ids)
            .associateBy { it.careRegNo }
            .toMutableMap()

        for (cmd in cmds) {
            val existing = map[cmd.careRegNo] // map에 저장해둔 Shelter

            if (existing != null) {
                if (existing.updTm.isBefore(cmd.updTm)) {
                    existing.updateFrom(cmd)
                }
            } else {
                val newShelter = create(cmd)
                shelterRepository.save(newShelter)
                map[cmd.careRegNo] = newShelter
            }
        }
    }

    fun findById(id: String): Shelter {
        return shelterRepository.findById(id)
            .orElseThrow { BusinessException(ShelterErrorCode.SHELTER_NOT_FOUND) }
    }

    fun getShelter(shelterId: String): ShelterRes {
        return shelterRepository.findById(shelterId)
            .map { shelter -> ShelterRes.from(shelter) }
            .orElseThrow { BusinessException(ShelterErrorCode.SHELTER_NOT_FOUND) }
    }

    fun getAllShelters(pageable: Pageable): ShelterListRes {
        val page = shelterRepository.findAll(pageable)

        return ShelterListRes(
            shelters = page.content.map { ShelterRes.from(it) },
            totalCount = page.totalElements,
            page = page.number,
            size = page.size,
            totalPages = page.totalPages
        )
    }

    fun searchShelter(keyword: String, pageable: Pageable): ShelterListRes {
        val page = shelterRepository.searchShelters(keyword, pageable)
        return ShelterListRes(
            shelters = page.content.map { ShelterRes.from(it) },
            totalCount = page.totalElements,
            page = page.number,
            size = page.size,
            totalPages = page.totalPages
        )
    }
}
