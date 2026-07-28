package com.team05.petmeeting.domain.ads.dto

data class GeminiRequest(
    val contents: List<Content>
) {
    data class Content(
        val parts: List<Part>
    )

    data class Part(
        val text: String
    )

    companion object {
        fun from(prompt: String): GeminiRequest {
            return GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(prompt))
                    )
                )
            )
        }
    }
}
