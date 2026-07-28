package com.team05.petmeeting.domain.user.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.mail")
class MailProperties {
    var async: Async = Async()
    var retry: Retry = Retry()

    class Async {
        var corePoolSize: Int = 2
        var maxPoolSize: Int = 5
        var queueCapacity: Int = 100
        var threadNamePrefix: String = "mail-"
    }

    class Retry {
        var maxAttempts: Int = 3
        var initialDelayMs: Long = 500
        var multiplier: Double = 2.0
        var maxDelayMs: Long = 5000
    }
}
