package com.team05.petmeeting.global.security.userdetails

import com.team05.petmeeting.domain.user.entity.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

class CustomOAuth2User(
    val user: User
) : OAuth2User {

    override fun getAttributes(): Map<String, Any> =
        emptyMap()

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority(user.role.name))

    override fun getName(): String =
        user.realname
}
