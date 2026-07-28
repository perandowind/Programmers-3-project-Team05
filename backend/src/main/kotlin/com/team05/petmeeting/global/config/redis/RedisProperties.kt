package com.team05.petmeeting.global.config.redis

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "spring.data.redis")
class RedisProperties {
    lateinit var host: String
    var port: Int = 0
}
