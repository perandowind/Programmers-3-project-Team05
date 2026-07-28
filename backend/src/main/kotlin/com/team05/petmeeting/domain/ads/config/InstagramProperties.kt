package com.team05.petmeeting.domain.ads.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "instagram")
class InstagramProperties {
    var userId: String? = null
    var accessToken: String? = null
}
