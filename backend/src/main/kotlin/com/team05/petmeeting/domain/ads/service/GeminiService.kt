package com.team05.petmeeting.domain.ads.service

interface GeminiService {
    fun generate(prompt: String): String
}
