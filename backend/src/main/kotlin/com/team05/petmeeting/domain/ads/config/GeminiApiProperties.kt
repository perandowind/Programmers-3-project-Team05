package com.team05.petmeeting.domain.ads.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gemini.api")
class GeminiApiProperties {
    var key: String? = null
    var url: String = ""
}
