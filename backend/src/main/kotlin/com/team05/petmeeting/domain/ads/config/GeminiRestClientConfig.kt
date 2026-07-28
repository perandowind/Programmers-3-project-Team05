package com.team05.petmeeting.domain.ads.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class GeminiRestClientConfig {
    @Bean
    fun geminiRestClient(): RestClient = RestClient.create()
}
