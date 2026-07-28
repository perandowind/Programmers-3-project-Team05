package com.team05.petmeeting.domain.user.service

import com.team05.petmeeting.domain.user.dto.auth.emailsignup.EmailSignupReq
import com.team05.petmeeting.domain.user.dto.auth.login.LoginAndRefreshRes
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.entity.UserAuth
import com.team05.petmeeting.domain.user.errorcode.UserErrorCode
import com.team05.petmeeting.domain.user.event.SignupOtpMailRequestedEvent
import com.team05.petmeeting.domain.user.provider.Provider
import com.team05.petmeeting.domain.user.refreshtoken.entity.RefreshToken
import com.team05.petmeeting.domain.user.refreshtoken.repository.RefreshTokenRepository
import com.team05.petmeeting.domain.user.repository.UserRepository
import com.team05.petmeeting.global.exception.BusinessException
import com.team05.petmeeting.global.security.util.JwtUtil
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.context.ApplicationEventPublisher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.util.ReflectionTestUtils
import java.util.*

class UserAuthServiceTest {

    private lateinit var userAuthService: UserAuthService
    private lateinit var userRepository: UserRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtUtil: JwtUtil
    private lateinit var otpService: OtpService
    private lateinit var eventPublisher: ApplicationEventPublisher

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        refreshTokenRepository = mock(RefreshTokenRepository::class.java)
        passwordEncoder = mock(PasswordEncoder::class.java)
        jwtUtil = mock(JwtUtil::class.java)
        otpService = mock(OtpService::class.java)
        eventPublisher = mock(ApplicationEventPublisher::class.java)

        userAuthService = UserAuthService(
            passwordEncoder,
            jwtUtil,
            eventPublisher,
            otpService,
            userRepository,
            refreshTokenRepository,
        )
    }

    @Test
    @DisplayName("sendSignupOtp - OTP 저장 후 메일 발송 이벤트를 발행")
    fun sendSignupOtp_success() {
        val email = "test@gmail.com"
        val code = "123456"

        `when`(otpService.saveSignupOtp(email)).thenReturn(code)

        userAuthService.sendSignupOtp(email)

        verify(otpService).checkCooldown(email)
        verify(otpService).saveSignupOtp(email)
        verify(eventPublisher).publishEvent(SignupOtpMailRequestedEvent(email, code))
    }

    @Test
    @DisplayName("verifyOtp - 성공")
    fun verifyOtp_success() {
        val email = "test@gmail.com"
        val code = "123456"

        `when`(otpService.getSignupOtp(email)).thenReturn(code)
        `when`(otpService.markVerifiedWithToken(email)).thenReturn("verify-token")

        val result = userAuthService.verifyOtp(email, code)

        assertThat(result).isEqualTo("verify-token")
        verify(otpService).clearOtp(email)
    }

    @Test
    @DisplayName("verifyOtp - OTP 없음 (만료)")
    fun verifyOtp_expired() {
        val email = "test@gmail.com"

        `when`(otpService.getSignupOtp(email)).thenReturn(null)

        assertThatThrownBy { userAuthService.verifyOtp(email, "123456") }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(UserErrorCode.EXPIRED_OTP)
    }

    @Test
    @DisplayName("verifyOtp - 코드 불일치")
    fun verifyOtp_invalid() {
        val email = "test@gmail.com"

        `when`(otpService.getSignupOtp(email)).thenReturn("123456")
        `when`(otpService.isExceededAttempts(email)).thenReturn(false)

        assertThatThrownBy { userAuthService.verifyOtp(email, "000000") }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(UserErrorCode.INVALID_OTP)

        verify(otpService).increaseAttempt(email)
    }

    @Test
    @DisplayName("verifyOtp - 시도 횟수 초과")
    fun verifyOtp_too_many_attempts() {
        val email = "test@gmail.com"

        `when`(otpService.getSignupOtp(email)).thenReturn("123456")
        `when`(otpService.isExceededAttempts(email)).thenReturn(true)

        assertThatThrownBy { userAuthService.verifyOtp(email, "000000") }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(UserErrorCode.TOO_MANY_OTP_ATTEMPTS)

        verify(otpService).increaseAttempt(email)
        verify(otpService).clearOtp(email)
    }

    @Test
    @DisplayName("signupAndLoginWithEmail - 성공")
    fun signup_success() {
        val token = "valid-token"
        val request = EmailSignupReq(token, "pw", "닉네임", "홍길동")
        val user = User.create("test@gmail.com", "닉네임", "홍길동").withId()

        `when`(otpService.getEmailByVerifyToken(token)).thenReturn("test@gmail.com")
        `when`(userRepository.findByEmail("test@gmail.com")).thenReturn(null)
        `when`(passwordEncoder.encode("pw")).thenReturn("encoded")
        `when`(jwtUtil.createToken(anyLong(), anyList())).thenReturn("accessToken")
        doReturn(user).`when`(userRepository).save(anyOr(user))

        val result = userAuthService.signupAndLoginWithEmail(request)

        assertThat(result.accessTokenRes.accessToken).isEqualTo("accessToken")
        verify(otpService).clearVerifiedByToken(token)
    }

    @Test
    @DisplayName("signupAndLoginWithEmail - 이미 가입된 이메일")
    fun signup_fail_duplicate() {
        val token = "token"
        val request = EmailSignupReq(token, "pw", "닉네임", "홍길동")
        val user = User.create("test@gmail.com", "닉네임", "홍길동")

        `when`(otpService.getEmailByVerifyToken(token)).thenReturn("test@gmail.com")
        `when`(userRepository.findByEmail("test@gmail.com")).thenReturn(user)

        assertThatThrownBy { userAuthService.signupAndLoginWithEmail(request) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(UserErrorCode.ALREADY_REGISTERED_EMAIL)
    }

    @Test
    @DisplayName("signupAndLoginWithEmail - verification token 없음")
    fun signup_fail_invalid_verification_token() {
        val token = "invalid-token"
        val request = EmailSignupReq(token, "pw", "닉네임", "홍길동")

        `when`(otpService.getEmailByVerifyToken(token)).thenReturn(null)

        assertThatThrownBy { userAuthService.signupAndLoginWithEmail(request) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(UserErrorCode.INVALID_VERIFICATION_TOKEN)
    }

    @Test
    @DisplayName("loginWithEmail - 성공")
    fun login_success() {
        val email = "test@gmail.com"
        val user = User.create(email, "닉네임", "홍길동").apply {
            addAuth(UserAuth.create(Provider.LOCAL, email, "encoded"))
        }.withId()

        `when`(userRepository.findByEmailWithAuths(email)).thenReturn(user)
        `when`(passwordEncoder.matches("pw", "encoded")).thenReturn(true)
        `when`(jwtUtil.createToken(anyLong(), anyList())).thenReturn("accessToken")

        val result = userAuthService.loginWithEmail(email, "pw")

        assertThat(result.accessTokenRes.accessToken).isEqualTo("accessToken")
        verify(refreshTokenRepository).save(anyOr(RefreshToken.create(user, UUID.randomUUID())))
    }

    @Test
    @DisplayName("loginWithEmail - 비밀번호 틀림")
    fun login_fail_wrong_password() {
        val email = "test@gmail.com"
        val user = User.create(email, "닉네임", "홍길동").apply {
            addAuth(UserAuth.create(Provider.LOCAL, email, "wrongPw"))
        }

        `when`(userRepository.findByEmailWithAuths(email)).thenReturn(user)
        `when`(passwordEncoder.matches("wrongPw", "encoded")).thenReturn(false)

        assertThatThrownBy { userAuthService.loginWithEmail(email, "wrongPw") }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(UserErrorCode.LOGIN_FAILED)
    }

    @Test
    @DisplayName("logout - refreshToken 존재")
    fun logout_success() {
        val token = UUID.randomUUID()
        val cookie = Cookie("refreshToken", token.toString())
        val request = mock(HttpServletRequest::class.java)

        `when`(request.cookies).thenReturn(arrayOf(cookie))

        userAuthService.logout(request)

        verify(refreshTokenRepository).deleteByToken(token)
    }

    @Test
    @DisplayName("logout - 쿠키 없음")
    fun logout_no_cookie() {
        val request = mock(HttpServletRequest::class.java)

        `when`(request.cookies).thenReturn(null)

        userAuthService.logout(request)

        verify(refreshTokenRepository, never()).deleteByToken(anyOr(UUID.randomUUID()))
    }

    @Test
    @DisplayName("refresh - 성공")
    fun refresh_success() {
        val token = UUID.randomUUID()
        val cookie = Cookie("refreshToken", token.toString())
        val request = mock(HttpServletRequest::class.java)
        val user = User.create("test@gmail.com", "닉네임", "홍길동").withId()
        val saved = RefreshToken.create(user, token)

        `when`(request.cookies).thenReturn(arrayOf(cookie))
        `when`(refreshTokenRepository.findByToken(token)).thenReturn(saved)
        `when`(jwtUtil.createToken(anyLong(), anyList())).thenReturn("newAccess")

        val result: LoginAndRefreshRes = userAuthService.refresh(request)

        assertThat(result.accessTokenRes.accessToken).isEqualTo("newAccess")
        verify(refreshTokenRepository).delete(saved)
        verify(refreshTokenRepository).save(anyOr(RefreshToken.create(user, UUID.randomUUID())))
    }

    private inline fun <reified T : Any> anyOr(value: T): T =
        any(T::class.java) ?: value

    private fun User.withId(id: Long = 1L): User =
        apply { ReflectionTestUtils.setField(this, "id", id) }
}
