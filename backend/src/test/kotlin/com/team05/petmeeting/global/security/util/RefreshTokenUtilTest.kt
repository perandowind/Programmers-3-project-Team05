package com.team05.petmeeting.global.security.util

import com.team05.petmeeting.global.security.util.RefreshTokenUtil.Companion.REFRESH_TOKEN_COOKIE_NAME
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletResponse

class RefreshTokenUtilTest {

    private val refreshTokenUtil = RefreshTokenUtil()

    @Test
    @DisplayName("refresh token 쿠키 생성 테스트")
    fun addTest() {
        val response = MockHttpServletResponse()
        val refreshToken = "test-refresh-token"

        refreshTokenUtil.add(response, refreshToken)

        val cookie = response.getCookie(REFRESH_TOKEN_COOKIE_NAME)

        assertThat(cookie).isNotNull()
        assertThat(cookie?.value).isEqualTo(refreshToken)
        assertThat(cookie?.isHttpOnly).isTrue()
        assertThat(cookie?.secure).isTrue()
        assertThat(cookie?.path).isEqualTo("/")
        assertThat(cookie?.maxAge).isEqualTo(7 * 24 * 60 * 60)
    }

    @Test
    @DisplayName("refresh token 쿠키 삭제 테스트")
    fun deleteTest() {
        val response = MockHttpServletResponse()

        refreshTokenUtil.delete(response)

        val cookie = response.getCookie(REFRESH_TOKEN_COOKIE_NAME)

        assertThat(cookie).isNotNull()
        assertThat(cookie?.value).isNull()
        assertThat(cookie?.isHttpOnly).isTrue()
        assertThat(cookie?.secure).isTrue()
        assertThat(cookie?.path).isEqualTo("/")
        assertThat(cookie?.maxAge).isEqualTo(0)
    }
}
