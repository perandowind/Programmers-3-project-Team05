package com.team05.petmeeting.domain.animal.service

import com.team05.petmeeting.domain.animal.client.AnimalApiClient
import com.team05.petmeeting.domain.animal.config.AnimalApiProperties
import com.team05.petmeeting.domain.animal.dto.external.AnimalApiResponse
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class AnimalExternalService(
    private val animalApiClient: AnimalApiClient,
    private val animalApiProperties: AnimalApiProperties,
) {
    // 일반 유기동물 조회를 날짜 조건 없이 호출한다.
    fun fetchAnimals(pageNo: Int, numOfRows: Int): AnimalApiResponse? =
        fetchAnimals(pageNo, numOfRows, null, null)

    // 일반 유기동물 조회 API에 기간 조건을 붙여 요청한다.
    fun fetchAnimals(
        pageNo: Int,
        numOfRows: Int,
        bgnde: LocalDate?,
        endde: LocalDate?,
    ): AnimalApiResponse? {
        val builder = createBaseAnimalUri(pageNo, numOfRows)

        if (bgnde != null) {
            builder.queryParam("bgnde", formatDate(bgnde))
        }
        if (endde != null) {
            builder.queryParam("endde", formatDate(endde))
        }

        return fetch(builder.toUriString())
    }

    // 마지막 수정일 기준으로 변경된 유기동물만 조회한다.
    fun fetchAnimalsByUpdatedDate(
        pageNo: Int,
        numOfRows: Int,
        bgupd: LocalDate?,
        enupd: LocalDate?,
    ): AnimalApiResponse? {
        val builder = createBaseAnimalUri(pageNo, numOfRows)

        if (bgupd != null) {
            builder.queryParam("bgupd", formatDate(bgupd))
        }
        if (enupd != null) {
            builder.queryParam("enupd", formatDate(enupd))
        }

        return fetch(builder.toUriString())
    }

    // 공통 쿼리 파라미터를 포함한 유기동물 조회 기본 URL을 만든다.
    private fun createBaseAnimalUri(pageNo: Int, numOfRows: Int): UriComponentsBuilder =
        UriComponentsBuilder.fromUriString(animalApiClient.abandonmentUrl)
            .queryParam("serviceKey", animalApiClient.serviceKey)
            .queryParam("pageNo", pageNo)
            .queryParam("numOfRows", numOfRows)
            .queryParam("_type", animalApiClient.returnType)

    // 완성된 URL로 외부 API를 호출하고 응답 DTO로 역직렬화한다.
    private fun fetch(url: String): AnimalApiResponse? =
        createRestClient()
            .get()
            .uri(url)
            .retrieve()
            .body(AnimalApiResponse::class.java)

    // 외부 API 전용 타임아웃 설정을 가진 RestClient를 생성한다.
    private fun createRestClient(): RestClient {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(animalApiProperties.timeoutMs)
        factory.setReadTimeout(animalApiProperties.timeoutMs)

        return RestClient.builder()
            .requestFactory(factory)
            .build()
    }

    // 외부 API가 요구하는 yyyyMMdd 형식으로 날짜를 변환한다.
    private fun formatDate(date: LocalDate): String = date.format(API_DATE_FORMATTER)

    companion object {
        private val API_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
    }
}
