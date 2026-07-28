package com.team05.petmeeting.domain.ads.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.team05.petmeeting.domain.ads.config.GeminiApiProperties
import com.team05.petmeeting.domain.ads.dto.GeminiRequest
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class GeminiServiceImpl(
    private val geminiApiProperties: GeminiApiProperties,
    private val objectMapper: ObjectMapper,
    private val geminiRestClient: RestClient,
) : GeminiService {
    override fun generate(prompt: String): String {
        val key = geminiApiProperties.key
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Gemini API key가 설정되지 않았습니다.")
        val url = geminiApiProperties.url
            .takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Gemini API URL이 설정되지 않았습니다.")

        val request = GeminiRequest.from(prompt)
        val response = try {
            geminiRestClient.post()
                .uri("$url?key=$key")
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(request))
                .retrieve()
                .body(String::class.java)
        } catch (e: Exception) {
            throw RuntimeException("Gemini 요청 실패", e)
        } ?: throw RuntimeException("Gemini 응답이 비어있습니다.")

        return try {
            objectMapper.readTree(response)
                .path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText()
                .trim()
        } catch (e: Exception) {
            throw RuntimeException("Gemini 파싱 실패", e)
        }
    }
}
