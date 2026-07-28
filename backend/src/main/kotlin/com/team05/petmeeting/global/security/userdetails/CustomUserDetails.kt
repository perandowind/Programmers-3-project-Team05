package com.team05.petmeeting.global.security.userdetails

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class CustomUserDetails(
    val userId: Long,
    private val authorities: List<GrantedAuthority>
) : UserDetails {

    override fun getUsername(): String = ""

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities

    override fun getPassword(): String = ""

    // 필요 시 아래 기본 메서드들도 override 가능
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true
}