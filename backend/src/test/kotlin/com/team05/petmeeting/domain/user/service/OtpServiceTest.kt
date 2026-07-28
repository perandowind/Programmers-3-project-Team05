package com.team05.petmeeting.domain.user.service

import com.team05.petmeeting.domain.user.errorcode.UserErrorCode
import com.team05.petmeeting.global.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.util.concurrent.TimeUnit

class OtpServiceTest {

    private lateinit var redisTemplate: StringRedisTemplate
    private lateinit var valueOperations: ValueOperations<String, String>
    private lateinit var otpService: OtpService

    @BeforeEach
    fun setUp() {
        redisTemplate = mock(StringRedisTemplate::class.java)
        @Suppress("UNCHECKED_CAST")
        valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, String>
        doReturn(valueOperations).`when`(redisTemplate).opsForValue()
        otpService = OtpService(redisTemplate)
    }

    @Test
    @DisplayName("OTP 저장 테스트")
    fun saveSignupOtp() {
        val email = "test@test.com"

        val code = otpService.saveSignupOtp(email)

        assertThat(code).isNotNull()
        verify(valueOperations).set(
            startsWith("otp:signup:"),
            anyString(),
            eq(5L),
            eq(TimeUnit.MINUTES),
        )
    }

    @Test
    @DisplayName("OTP 조회 테스트")
    fun getSignupOtp() {
        val email = "test@test.com"
        `when`(valueOperations.get("otp:signup:$email")).thenReturn("123456")

        val result = otpService.getSignupOtp(email)

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo("123456")
    }

    @Test
    @DisplayName("시도 횟수 증가 테스트")
    fun increaseAttempt() {
        val email = "test@test.com"

        `when`(valueOperations.increment(anyString())).thenReturn(1L)

        otpService.increaseAttempt(email)

        verify(valueOperations).increment("otp:attempt:$email")
        verify(redisTemplate).expire("otp:attempt:$email", 5L, TimeUnit.MINUTES)
    }

    @Test
    @DisplayName("시도 횟수 초과 여부 - 초과")
    fun isExceededAttempts_true() {
        val email = "test@test.com"

        `when`(valueOperations.get("otp:attempt:$email")).thenReturn("6")

        val result = otpService.isExceededAttempts(email)

        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("시도 횟수 초과 여부 - 정상")
    fun isExceededAttempts_false() {
        val email = "test@test.com"

        `when`(valueOperations.get("otp:attempt:$email")).thenReturn("3")

        val result = otpService.isExceededAttempts(email)

        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("OTP 삭제 테스트")
    fun clearOtp() {
        val email = "test@test.com"

        otpService.clearOtp(email)

        verify(redisTemplate).delete("otp:signup:$email")
        verify(redisTemplate).delete("otp:attempt:$email")
    }

    @Test
    @DisplayName("인증 토큰 생성 테스트")
    fun markVerifiedWithToken() {
        val email = "test@test.com"

        val token = otpService.markVerifiedWithToken(email)

        assertThat(token).isNotNull()
        verify(valueOperations).set(
            startsWith("otp:verified:"),
            eq(email),
            eq(60L),
            eq(TimeUnit.MINUTES),
        )
    }

    @Test
    @DisplayName("토큰으로 이메일 조회")
    fun getEmailByVerifyToken() {
        val token = "abc"

        `when`(valueOperations.get("otp:verified:$token")).thenReturn("test@test.com")

        val result = otpService.getEmailByVerifyToken(token)

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo("test@test.com")
    }

    @Test
    @DisplayName("토큰 삭제 테스트")
    fun clearVerifiedByToken() {
        val token = "abc"

        otpService.clearVerifiedByToken(token)

        verify(redisTemplate).delete("otp:verified:$token")
    }

    @Test
    @DisplayName("쿨다운 체크 - 이미 존재하면 예외")
    fun checkCooldown_throw() {
        val email = "test@test.com"

        `when`(redisTemplate.hasKey("otp:cooldown:$email")).thenReturn(true)

        assertThatThrownBy { otpService.checkCooldown(email) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(UserErrorCode.TOO_MANY_OTP_REQUEST)
    }

    @Test
    @DisplayName("쿨다운 체크 - 정상")
    fun checkCooldown_success() {
        val email = "test@test.com"

        `when`(redisTemplate.hasKey("otp:cooldown:$email")).thenReturn(false)

        otpService.checkCooldown(email)

        verify(valueOperations).set(
            "otp:cooldown:$email",
            "1",
            60L,
            TimeUnit.SECONDS,
        )
    }
}
