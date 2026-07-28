package com.team05.petmeeting.global.security.oauth

import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.refreshtoken.entity.RefreshToken
import com.team05.petmeeting.domain.user.refreshtoken.repository.RefreshTokenRepository
import com.team05.petmeeting.global.security.userdetails.CustomOAuth2User
import com.team05.petmeeting.global.security.util.RefreshTokenUtil
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.core.Authentication
import java.util.UUID

class OAuth2SuccessHandlerTest {

    private lateinit var refreshTokenUtil: RefreshTokenUtil
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var authentication: Authentication
    private lateinit var successHandler: OAuth2SuccessHandler

    @BeforeEach
    fun setUp() {
        refreshTokenUtil = mock(RefreshTokenUtil::class.java)
        refreshTokenRepository = mock(RefreshTokenRepository::class.java)
        request = mock(HttpServletRequest::class.java)
        response = mock(HttpServletResponse::class.java)
        authentication = mock(Authentication::class.java)
        successHandler = OAuth2SuccessHandler(refreshTokenUtil, refreshTokenRepository, "http://localhost:3000")
    }

    @Test
    fun OAuth2_로그인_성공시_리프레시토큰_생성_쿠키설정_리다이렉트() {
        val user = User.create("test@test.com", "nick", "real")
        val customOAuth2User = CustomOAuth2User(user)

        `when`(authentication.principal).thenReturn(customOAuth2User)

        successHandler.onAuthenticationSuccess(request, response, authentication)

        verify(refreshTokenRepository, times(1))
            .save(anyOr(RefreshToken.create(user, UUID.randomUUID())))
        verify(refreshTokenUtil, times(1))
            .add(anyOr(response), anyString())
        verify(response, times(1))
            .sendRedirect("http://localhost:3000?oauth=success")
    }

    private inline fun <reified T : Any> anyOr(value: T): T =
        any(T::class.java) ?: value
}
