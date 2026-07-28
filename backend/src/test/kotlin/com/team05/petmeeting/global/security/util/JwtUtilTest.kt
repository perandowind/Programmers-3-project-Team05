package com.team05.petmeeting.global.security.util

import com.team05.petmeeting.global.security.userdetails.CustomUserDetails
import io.jsonwebtoken.Claims
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import javax.crypto.SecretKey

class JwtUtilTest {

    private lateinit var jwtUtil: JwtUtil
    private lateinit var secretKey: SecretKey

    @BeforeEach
    fun setUp() {
        secretKey = Keys.hmacShaKeyFor("testtesttesttesttesttesttesttest".toByteArray())
        jwtUtil = JwtUtil(secretKey, 1000L * 60 * 60)
    }

    @Test
    @DisplayName("토큰 생성 테스트")
    fun createTokenTest() {
        val userId = 1L
        val roles = listOf("ROLE_USER")

        val token = jwtUtil.createToken(userId, roles)

        assertThat(token).isNotNull()
        assertThat(token).isNotEmpty()
    }

    @Test
    @DisplayName("토큰 파싱 테스트")
    fun parseTokenTest() {
        val userId = 1L
        val roles = listOf("ROLE_USER")
        val token = jwtUtil.createToken(userId, roles)

        val claims: Claims = jwtUtil.parseToken(token)

        assertThat(claims.subject).isEqualTo("1")
        assertThat(claims["roles"]).isNotNull()
    }

    @Test
    @DisplayName("Authentication 생성 테스트")
    fun getAuthenticationTest() {
        val userId = 1L
        val roles = listOf("ROLE_USER")
        val token = jwtUtil.createToken(userId, roles)
        val claims = jwtUtil.parseToken(token)

        val authentication: Authentication = jwtUtil.getAuthentication(claims)

        assertThat(authentication).isNotNull()
        assertThat(authentication.isAuthenticated).isTrue()
        assertThat(authentication.principal).isInstanceOf(CustomUserDetails::class.java)
        assertThat(authentication.authorities)
            .extracting("authority")
            .containsExactly("ROLE_USER")
    }
}
