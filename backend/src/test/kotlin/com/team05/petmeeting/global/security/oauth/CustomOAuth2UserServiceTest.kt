package com.team05.petmeeting.global.security.oauth

import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.entity.UserAuth
import com.team05.petmeeting.domain.user.provider.Provider
import com.team05.petmeeting.domain.user.repository.UserRepository
import com.team05.petmeeting.global.security.userdetails.CustomOAuth2User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User

class CustomOAuth2UserServiceTest {

    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
    }

    @Test
    @DisplayName("신규 유저 - User + GoogleAuth 생성")
    fun 신규_유저_생성() {
        val email = "test@gmail.com"
        val sub = "google-123"
        val name = "tester"
        val service = serviceWithOAuthUser(email, sub, name)
        val request = mock(OAuth2UserRequest::class.java)

        `when`(userRepository.findByEmailWithAuths(email)).thenReturn(null)

        val result = service.loadUser(request)

        verify(userRepository).save(anyOr(User.create(email, "nick", name)))
        assertThat(result).isNotNull()
        assertThat((result as CustomOAuth2User).user).isNotNull()
    }

    @Test
    @DisplayName("기존 유저 + GoogleAuth 없음 -> GoogleAuth 추가")
    fun 기존유저_구글연동없음() {
        val email = "test@gmail.com"
        val sub = "google-123"
        val name = "tester"
        val user = User.create(email, "nick", name).apply {
            addAuth(UserAuth.create(Provider.LOCAL, email, "pw"))
        }
        val service = serviceWithOAuthUser(email, sub, name)
        val request = mock(OAuth2UserRequest::class.java)

        `when`(userRepository.findByEmailWithAuths(email)).thenReturn(user)

        service.loadUser(request)

        assertThat(user.userAuths).anyMatch { it.provider == Provider.GOOGLE }
    }

    @Test
    @DisplayName("기존 유저 + GoogleAuth 있음 -> 추가 안함")
    fun 기존유저_구글연동있음() {
        val email = "test@gmail.com"
        val sub = "google-123"
        val name = "tester"
        val user = User.create(email, "nick", name).apply {
            addAuth(UserAuth.create(Provider.GOOGLE, sub, null))
        }
        val beforeSize = user.userAuths.size
        val service = serviceWithOAuthUser(email, sub, name)
        val request = mock(OAuth2UserRequest::class.java)

        `when`(userRepository.findByEmailWithAuths(email)).thenReturn(user)

        service.loadUser(request)

        assertThat(user.userAuths).hasSize(beforeSize)
    }

    private fun serviceWithOAuthUser(
        email: String,
        sub: String,
        name: String,
    ): CustomOAuth2UserService =
        StubOAuth2UserService(
            userRepository,
            DefaultOAuth2User(
                emptyList<GrantedAuthority>(),
                mapOf("email" to email, "sub" to sub, "name" to name),
                "email",
            ),
        )

    private inline fun <reified T : Any> anyOr(value: T): T =
        any(T::class.java) ?: value

    private class StubOAuth2UserService(
        userRepository: UserRepository,
        private val oauthUser: OAuth2User,
    ) : CustomOAuth2UserService(userRepository) {

        override fun getOAuthUser(request: OAuth2UserRequest): OAuth2User = oauthUser
    }
}
