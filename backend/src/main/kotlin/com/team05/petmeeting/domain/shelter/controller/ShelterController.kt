package com.team05.petmeeting.domain.shelter.controller

import com.team05.petmeeting.domain.shelter.dto.ShelterListRes
import com.team05.petmeeting.domain.shelter.dto.ShelterRes
import com.team05.petmeeting.domain.shelter.service.ShelterService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.RequestParam

@RestController
@RequestMapping("/api/v1/shelters")
class ShelterController(private val shelterService: ShelterService) {
    @Operation(summary = "보호소 조회")
    @GetMapping("/{shelterId}")
    fun getShelter(
        @PathVariable shelterId: String
    ): ResponseEntity<ShelterRes> {
        val res = shelterService.getShelter(shelterId)
        return ResponseEntity.ok(res)
    }

    @Operation(summary = "전체 보호소 조회")
    @GetMapping("/")
    fun getShelterList(pageable: Pageable): ResponseEntity<ShelterListRes> {
        val res = shelterService.getAllShelters(pageable)
        return ResponseEntity.ok(res)
    }

    @Operation(summary = "보호소 검색")
    @GetMapping("/search")
    fun searchShelters(
        @RequestParam keyword: String,
        pageable: Pageable
    ) : ResponseEntity<ShelterListRes>{
        val keyword : String = keyword.trim()
        if (keyword.isEmpty()) return ResponseEntity.ok(shelterService.getAllShelters(pageable))
        val res = shelterService.searchShelter(keyword, pageable)
        return ResponseEntity.ok(res)
    }
}
