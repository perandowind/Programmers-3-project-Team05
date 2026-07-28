package com.team05.petmeeting.domain.ads.controller

import com.team05.petmeeting.domain.ads.dto.AdsPostRequestRes
import com.team05.petmeeting.domain.ads.service.AdsService
import com.team05.petmeeting.domain.animal.entity.Animal
import com.team05.petmeeting.global.rsdata.RsData
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/ads")
class AdsController(
    private val adsService: AdsService
) {
    // Top N 동물 조회
    @GetMapping("/top-animals")
    fun getTopAnimals(
        @RequestParam(defaultValue = "3") n: Int
    ): RsData<List<Animal>> {
        val topAnimals = adsService.getTopAnimals(n)
        return RsData("Top N 동물 조회 성공", "200", topAnimals)
    }

    // 인스타그램 업로드 없이 카드뉴스를 생성하고 승인 대기 요청으로 저장
    @PostMapping("/card-news/preview")
    fun previewCardNews(
        @RequestParam(defaultValue = "3") n: Int
    ): RsData<List<AdsPostRequestRes>> {
        val requests = adsService.createCardNewsPostRequests(n)
        return RsData("카드뉴스 승인 요청 생성 성공", "200", requests)
    }
}
