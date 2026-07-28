package com.team05.petmeeting.domain.user.service

import com.team05.petmeeting.domain.user.config.MailProperties
import com.team05.petmeeting.domain.user.event.SignupOtpMailRequestedEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class SignupOtpMailEventListenerTest {

    private lateinit var mailService: MailService
    private lateinit var otpService: OtpService
    private lateinit var mailProperties: MailProperties
    private lateinit var mailRetryScheduler: ScheduledExecutorService
    private lateinit var listener: SignupOtpMailEventListener

    @BeforeEach
    fun setUp() {
        mailService = mock(MailService::class.java)
        otpService = mock(OtpService::class.java)
        mailRetryScheduler = mock(ScheduledExecutorService::class.java)
        mailProperties = MailProperties().apply {
            retry.maxAttempts = 3
            retry.initialDelayMs = 500
            retry.multiplier = 1.0
            retry.maxDelayMs = 500
        }
        doAnswer { invocation ->
            invocation.getArgument<Runnable>(0).run()
            mock(ScheduledFuture::class.java)
        }.`when`(mailRetryScheduler).schedule(anyOr(Runnable {}), anyLong(), anyOr(TimeUnit.MILLISECONDS))

        listener = SignupOtpMailEventListener(
            mailService,
            otpService,
            mailProperties,
            Executor { it.run() },
            mailRetryScheduler,
        )
    }

    @Test
    @DisplayName("handle - 일시 실패 후 재시도에 성공하면 OTP를 삭제하지 않는다")
    fun handle_retrySuccess() {
        val event = SignupOtpMailRequestedEvent("test@test.com", "123456")

        doThrow(RuntimeException("temporary"))
            .doNothing()
            .`when`(mailService).sendMail(event.email, event.code)

        listener.handle(event)

        verify(mailService, times(2)).sendMail(event.email, event.code)
        verify(mailRetryScheduler).schedule(anyOr(Runnable {}), anyLong(), anyOr(TimeUnit.MILLISECONDS))
        verify(otpService, never()).clearSignupOtpIfMatches(anyString(), anyString())
    }

    @Test
    @DisplayName("handle - 모든 재시도 실패 시 해당 OTP를 삭제한다")
    fun handle_retryFailed() {
        val event = SignupOtpMailRequestedEvent("test@test.com", "123456")

        doThrow(RuntimeException("failed"))
            .`when`(mailService).sendMail(event.email, event.code)

        listener.handle(event)

        verify(mailService, times(3)).sendMail(event.email, event.code)
        verify(mailRetryScheduler, times(2)).schedule(anyOr(Runnable {}), anyLong(), anyOr(TimeUnit.MILLISECONDS))
        verify(otpService).clearSignupOtpIfMatches(event.email, event.code)
    }

    private inline fun <reified T : Any> anyOr(value: T): T =
        org.mockito.ArgumentMatchers.any(T::class.java) ?: value
}
