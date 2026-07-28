package com.team05.petmeeting.domain.animal

import com.team05.petmeeting.domain.animal.client.AnimalApiClient
import com.team05.petmeeting.domain.animal.config.AnimalApiProperties
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class AnimalApiClientTest {

    @Test
    @DisplayName("유기동물 목록 조회 URL 생성")
    fun abandonmentUrl() {
        val animalApiClient = createAnimalApiClient()

        val result = animalApiClient.abandonmentUrl

        assertEquals("$BASE_URL/abandonmentPublic_v2", result)
    }

    @Test
    @DisplayName("시도 조회 URL 생성")
    fun sidoUrl() {
        val animalApiClient = createAnimalApiClient()

        val result = animalApiClient.sidoUrl

        assertEquals("$BASE_URL/sido_v2", result)
    }

    @Test
    @DisplayName("시군구 조회 URL 생성")
    fun sigunguUrl() {
        val animalApiClient = createAnimalApiClient()

        val result = animalApiClient.sigunguUrl

        assertEquals("$BASE_URL/sigungu_v2", result)
    }

    @Test
    @DisplayName("보호소 조회 URL 생성")
    fun shelterUrl() {
        val animalApiClient = createAnimalApiClient()

        val result = animalApiClient.shelterUrl

        assertEquals("$BASE_URL/shelter_v2", result)
    }

    @Test
    @DisplayName("품종 조회 URL을 생성")
    fun kindUrl() {
        val animalApiClient = createAnimalApiClient()

        val result = animalApiClient.kindUrl

        assertEquals("$BASE_URL/kind_v2", result)
    }

    @Test
    @DisplayName("서비스키 반환")
    fun serviceKey() {
        val animalApiClient = createAnimalApiClient()

        val result = animalApiClient.serviceKey

        assertEquals("test-key", result)
    }

    @Test
    @DisplayName("응답 타입 반환")
    fun returnType() {
        val animalApiClient = createAnimalApiClient()

        val result = animalApiClient.returnType

        assertEquals("json", result)
    }

    private fun createAnimalApiClient(): AnimalApiClient {
        // 각 테스트가 동일한 base URL, service key, return type 조합을 재사용하도록 묶어둔다.
        val properties = AnimalApiProperties().apply {
            baseUrl = BASE_URL
            serviceKey = "test-key"
            returnType = "json"
        }
        return AnimalApiClient(properties)
    }

    companion object {
        private const val BASE_URL = "https://apis.data.go.kr/1543061/abandonmentPublicService_v2"
    }
}
