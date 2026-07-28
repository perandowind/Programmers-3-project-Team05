package com.team05.petmeeting.domain.animal.client

import com.team05.petmeeting.domain.animal.config.AnimalApiProperties
import org.springframework.stereotype.Component

@Component
class AnimalApiClient(
    private val properties: AnimalApiProperties,
) {
    // 유기동물 정보 조회 API URL
    val abandonmentUrl: String
        get() = properties.baseUrl + ABANDONMENT_PATH

    // 시도 정보 조회 API URL
    val sidoUrl: String
        get() = properties.baseUrl + SIDO_PATH

    // 시군구 정보 조회 API URL
    val sigunguUrl: String
        get() = properties.baseUrl + SIGUNGU_PATH

    // 보호소 정보 조회 API URL
    val shelterUrl: String
        get() = properties.baseUrl + SHELTER_PATH

    // 품종 정보 조회 API URL
    val kindUrl: String
        get() = properties.baseUrl + KIND_PATH

    // API 인증에 필요한 서비스 키
    val serviceKey: String
        get() = properties.serviceKey

    // API 응답 형식
    val returnType: String
        get() = properties.returnType

    companion object {
        private const val ABANDONMENT_PATH = "/abandonmentPublic_v2"
        private const val SIDO_PATH = "/sido_v2"
        private const val SIGUNGU_PATH = "/sigungu_v2"
        private const val SHELTER_PATH = "/shelter_v2"
        private const val KIND_PATH = "/kind_v2"
    }
}
