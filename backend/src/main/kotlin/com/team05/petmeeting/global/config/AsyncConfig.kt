package com.team05.petmeeting.global.config

import com.team05.petmeeting.domain.user.config.MailProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicInteger

@Configuration
class AsyncConfig(
    private val mailProperties: MailProperties,
) {

    @Bean
    fun mailTaskExecutor(): Executor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = mailProperties.async.corePoolSize.coerceAtLeast(1)
            maxPoolSize = mailProperties.async.maxPoolSize.coerceAtLeast(corePoolSize)
            queueCapacity = mailProperties.async.queueCapacity.coerceAtLeast(0)
            setThreadNamePrefix(mailProperties.async.threadNamePrefix)
            initialize()
        }

    @Bean(destroyMethod = "shutdown")
    fun mailRetryScheduler(): ScheduledExecutorService {
        val sequence = AtomicInteger(1)
        return Executors.newScheduledThreadPool(1) { task ->
            Thread(task, "${mailProperties.async.threadNamePrefix}retry-${sequence.getAndIncrement()}")
        }
    }
}
