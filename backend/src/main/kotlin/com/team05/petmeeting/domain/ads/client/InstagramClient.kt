package com.team05.petmeeting.domain.ads.client

import com.team05.petmeeting.domain.ads.config.InstagramProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.util.UriComponentsBuilder

@Component
class InstagramClient(
    private val properties: InstagramProperties
) {
    // 1단계: 미디어 컨테이너 생성
    fun createMediaContainer(imageUrl: String?, caption: String?): String? {
        val userId = requireInstagramProperty(properties.userId, "Instagram User ID")
        val accessToken = requireInstagramProperty(properties.accessToken, "Instagram Access Token")
        val mediaImageUrl = imageUrl?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("인스타그램 이미지 URL이 비어있습니다.")
        val url = UriComponentsBuilder
            .fromUriString("$BASE_URL/$userId/media")
            .queryParam("image_url", mediaImageUrl)
            .queryParam("caption", caption.orEmpty())
            .queryParam("access_token", accessToken)
            .build()
            .toUriString()

        try {
            return RestClient.create()
                .post()
                .uri(url)
                .retrieve()
                .body(String::class.java)
        } catch (e: RestClientResponseException) {
            throw IllegalStateException(
                "미디어 컨테이너 생성 실패: ${e.responseBodyAsString}", e
            )
        }
    }

    // 2단계: 미디어 게시
    fun publishMedia(containerId: String): String? {
        val userId = requireInstagramProperty(properties.userId, "Instagram User ID")
        val accessToken = requireInstagramProperty(
            properties.accessToken,
            "Instagram Access Token"
        )
        val url = "$BASE_URL/$userId/media_publish"

        val body = "creation_id=$containerId&access_token=$accessToken"

        try {
            return RestClient.create()
                .post()
                .uri(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(body)
                .retrieve()
                .body(String::class.java)
        } catch (e: RestClientResponseException) {
            throw IllegalStateException(
                "미디어 게시 실패: ${e.responseBodyAsString}", e
            )
        }
    }

    fun getContainerStatus(containerId: String): String {
        val accessToken = requireInstagramProperty(
            properties.accessToken,
            "Instagram Access Token"
        )
        val url = UriComponentsBuilder
            .fromUriString("$BASE_URL/$containerId")
            .queryParam("fields", "status_code")
            .queryParam("access_token", accessToken)
            .build()
            .toUriString()

        try {
            val response = RestClient.create()
                .get()
                .uri(url)
                .retrieve()
                .body(String::class.java)

            return response ?: throw IllegalStateException("인스타그램 컨테이너 상태 응답이 비어있습니다.")
        } catch (e: RestClientResponseException) {
            throw IllegalStateException(
                "인스타그램 컨테이너 상태 조회 실패: ${e.responseBodyAsString}",
                e
            )
        }
    }

    private fun requireInstagramProperty(value: String?, name: String): String {
        return value?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("${name}가 설정되지 않았습니다.")
    }

    companion object {
        private const val BASE_URL = "https://graph.facebook.com/v19.0"
    }
}
