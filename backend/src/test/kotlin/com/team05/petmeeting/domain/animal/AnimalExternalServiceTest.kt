package com.team05.petmeeting.domain.animal

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.team05.petmeeting.domain.animal.client.AnimalApiClient
import com.team05.petmeeting.domain.animal.config.AnimalApiProperties
import com.team05.petmeeting.domain.animal.dto.external.AnimalApiResponse
import com.team05.petmeeting.domain.animal.service.AnimalExternalService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference

class AnimalExternalServiceTest {

    private var server: HttpServer? = null
    private lateinit var capturedQuery: AtomicReference<String>
    private lateinit var animalExternalService: AnimalExternalService

    @BeforeEach
    @Throws(IOException::class)
    fun setUp() {
        capturedQuery = AtomicReference()
        // 실제 외부 호출 대신 로컬 HTTP 서버로 요청 쿼리와 응답 매핑을 검증한다.
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/abandonmentPublic_v2", this@AnimalExternalServiceTest::handleAnimalRequest)
            start()
        }

        val properties = AnimalApiProperties().apply {
            baseUrl = "http://localhost:${server!!.address.port}"
            serviceKey = "test-key"
            returnType = "json"
            timeoutMs = 1000
        }

        val animalApiClient = AnimalApiClient(properties)
        animalExternalService = AnimalExternalService(animalApiClient, properties)
    }

    @AfterEach
    fun tearDown() {
        server?.stop(0)
    }

    @Test
    @DisplayName("일반 동물 조회 요청 파라미터와 응답 매핑")
    fun fetchAnimals() {
        val response: AnimalApiResponse? = animalExternalService.fetchAnimals(
            2,
            30,
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 30),
        )

        val queryParams = parseQuery(capturedQuery.get())
        assertThat(queryParams)
            .containsEntry("serviceKey", "test-key")
            .containsEntry("pageNo", "2")
            .containsEntry("numOfRows", "30")
            .containsEntry("_type", "json")
            .containsEntry("bgnde", "20260401")
            .containsEntry("endde", "20260430")

        // 테스트 응답 JSON은 완전한 구조로 고정돼 있으므로 여기서는 non-null 단언으로 검증을 단순화한다.
        val items = response!!.response!!.body!!.items!!.item!!
        assertThat(items).hasSize(1)
        val item = items.first()
        assertThat(item.desertionNo).isEqualTo("D-001")
        assertThat(item.processState).isEqualTo("보호중")
        assertThat(item.careRegNo).isEqualTo("CARE-001")
    }

    @Test
    @DisplayName("수정일 기준 동물 조회 요청 파라미터")
    fun fetchAnimalsByUpdatedDate() {
        val response = animalExternalService.fetchAnimalsByUpdatedDate(
            1,
            50,
            LocalDate.of(2026, 4, 20),
            LocalDate.of(2026, 4, 23),
        )

        val queryParams = parseQuery(capturedQuery.get())
        assertThat(queryParams)
            .containsEntry("serviceKey", "test-key")
            .containsEntry("pageNo", "1")
            .containsEntry("numOfRows", "50")
            .containsEntry("_type", "json")
            .containsEntry("bgupd", "20260420")
            .containsEntry("enupd", "20260423")
        assertThat(response!!.response!!.body!!.items!!.item!!).hasSize(1)
    }

    @Throws(IOException::class)
    private fun handleAnimalRequest(exchange: HttpExchange) {
        // 서비스가 실제로 보낸 raw query string을 그대로 잡아두고 assertion에 사용한다.
        capturedQuery.set(exchange.requestURI.rawQuery)

        val responseBody = animalApiResponseJson().toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders["Content-Type"] = listOf("application/json; charset=UTF-8")
        exchange.sendResponseHeaders(200, responseBody.size.toLong())
        exchange.responseBody.use { responseStream: OutputStream ->
            responseStream.write(responseBody)
        }
    }

    private fun animalApiResponseJson(): String =
        """
        {
          "response": {
            "header": {
              "reqNo": 1,
              "resultCode": "00",
              "resultMsg": "NORMAL SERVICE"
            },
            "body": {
              "items": {
                "item": [
                  {
                    "desertionNo": "D-001",
                    "processState": "보호중",
                    "noticeNo": "NOTICE-001",
                    "noticeEdt": "20260430",
                    "happenPlace": "테스트 장소",
                    "upKindNm": "개",
                    "kindFullNm": "믹스견",
                    "colorCd": "흰색",
                    "age": "2024(년생)",
                    "weight": "5(Kg)",
                    "sexCd": "M",
                    "popfile1": "https://example.com/image1.jpg",
                    "popfile2": "https://example.com/image2.jpg",
                    "specialMark": "온순함",
                    "careOwnerNm": "담당자",
                    "careNm": "테스트 보호소",
                    "careAddr": "테스트 주소",
                    "careTel": "010-1234-5678",
                    "updTm": "2026-04-21 10:00:00.0",
                    "careRegNo": "CARE-001",
                    "orgNm": "테스트 기관"
                  }
                ]
              },
              "numOfRows": 1,
              "pageNo": 1,
              "totalCount": 1
            }
          }
        }
        """.trimIndent()

    private fun parseQuery(rawQuery: String): Map<String, String> =
        // HttpServer가 넘긴 raw query를 key/value 맵으로 바꿔 요청 파라미터를 비교한다.
        rawQuery.split("&")
            .map { it.split("=", limit = 2) }
            .associate { parts ->
                parts[0] to parts.getOrElse(1) { "" }
            }
}
