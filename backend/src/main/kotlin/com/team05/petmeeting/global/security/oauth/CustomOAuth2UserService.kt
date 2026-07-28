package com.team05.petmeeting.global.security.oauth

import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.entity.UserAuth
import com.team05.petmeeting.domain.user.provider.Provider
import com.team05.petmeeting.domain.user.repository.UserRepository
import com.team05.petmeeting.global.security.userdetails.CustomOAuth2User
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
) : DefaultOAuth2UserService() {

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val googleUser = getOAuthUser(userRequest)
        log.info("user: {}", googleUser.attributes)

        val email = googleUser.requiredAttribute<String>("email")
        val providerId = googleUser.requiredAttribute<String>("sub")
        val name = googleUser.requiredAttribute<String>("name")

        val user = userRepository.findByEmailWithAuths(email)
            ?.apply { addGoogleAuthIfAbsent(providerId) }
            ?: User.create(email, generateNickname(name), name).apply {
                addAuth(UserAuth.create(Provider.GOOGLE, providerId, null))
            }

        userRepository.save(user)

        return CustomOAuth2User(user)
    }

    private fun User.addGoogleAuthIfAbsent(providerId: String) {
        if (userAuths.none { it.provider == Provider.GOOGLE }) {
            addAuth(UserAuth.create(Provider.GOOGLE, providerId, null))
        }
    }

    private fun generateNickname(name: String): String =
        "${name}_${Random.nextInt(10_000)}"

    private fun <T> OAuth2User.requiredAttribute(name: String): T =
        requireNotNull(getAttribute(name)) { "OAuth2 attribute '$name' is required." }

    protected fun getOAuthUser(request: OAuth2UserRequest): OAuth2User =
        super.loadUser(request)

    companion object {
        private val log = LoggerFactory.getLogger(CustomOAuth2UserService::class.java)
    }
}
