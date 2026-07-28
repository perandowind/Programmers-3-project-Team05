package com.team05.petmeeting.domain.user.service

import com.team05.petmeeting.domain.user.config.MailProperties
import com.team05.petmeeting.domain.user.event.SignupOtpMailRequestedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.min

@Component
class SignupOtpMailEventListener(
    private val mailService: MailService,
    private val otpService: OtpService,
    private val mailProperties: MailProperties,
    @param:Qualifier("mailTaskExecutor")
    private val mailTaskExecutor: Executor,
    @param:Qualifier("mailRetryScheduler")
    private val mailRetryScheduler: ScheduledExecutorService,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    // 트랜잭션 커밋 성공 후에만 실행
    fun handle(event: SignupOtpMailRequestedEvent) {
        submitAttempt(event, attempt = 1)
    }

    private fun submitAttempt(event: SignupOtpMailRequestedEvent, attempt: Int) {
        try {
            mailTaskExecutor.execute { sendAttempt(event, attempt) }
        } catch (e: RuntimeException) {
            handleFinalFailure(event, e)
        }
    }

    private fun sendAttempt(event: SignupOtpMailRequestedEvent, attempt: Int) {
        val maxAttempts = mailProperties.retry.maxAttempts.coerceAtLeast(1)

        try {
            mailService.sendMail(event.email, event.code)
            if (attempt > 1) {
                log.info("Signup OTP mail sent after retry. email={}, attempt={}", event.email, attempt)
            }
        } catch (e: RuntimeException) {
            if (attempt >= maxAttempts) {
                handleFinalFailure(event, e)
                return
            }

            val delayMs = retryDelayAfterFailure(attempt)
            log.warn(
                "Signup OTP mail send failed, retrying. email={}, attempt={}/{}, delayMs={}",
                event.email,
                attempt,
                maxAttempts,
                delayMs,
                e,
            )
            scheduleRetry(event, attempt + 1, delayMs)
        }
    }

    private fun scheduleRetry(event: SignupOtpMailRequestedEvent, nextAttempt: Int, delayMs: Long) {
        try {
            mailRetryScheduler.schedule(
                { submitAttempt(event, nextAttempt) },
                delayMs.coerceAtLeast(0),
                TimeUnit.MILLISECONDS,
            )
        } catch (e: RuntimeException) {
            handleFinalFailure(event, e)
        }
    }

    private fun handleFinalFailure(event: SignupOtpMailRequestedEvent, cause: Throwable) {
        val deleted = otpService.clearSignupOtpIfMatches(event.email, event.code)
        log.error(
            "Signup OTP mail failed after retries. email={}, otpDeleted={}",
            event.email,
            deleted,
            cause,
        )
    }

    private fun retryDelayAfterFailure(failedAttempt: Int): Long {
        val initialDelayMs = mailProperties.retry.initialDelayMs.coerceAtLeast(0)
        val maxDelayMs = mailProperties.retry.maxDelayMs.coerceAtLeast(0)
        if (initialDelayMs == 0L || maxDelayMs == 0L) {
            return 0
        }

        var delayMs = min(initialDelayMs, maxDelayMs)
        repeat((failedAttempt - 1).coerceAtLeast(0)) {
            delayMs = nextDelay(delayMs)
        }
        return delayMs
    }

    private fun nextDelay(currentDelayMs: Long): Long {
        val maxDelayMs = mailProperties.retry.maxDelayMs.coerceAtLeast(0)
        val multiplied = (currentDelayMs * mailProperties.retry.multiplier).toLong()
        return min(maxDelayMs, multiplied.coerceAtLeast(currentDelayMs))
    }

    companion object {
        private val log = LoggerFactory.getLogger(SignupOtpMailEventListener::class.java)
    }
}
