package com.team05.petmeeting.global.security.util

import com.team05.petmeeting.global.security.userdetails.CustomUserDetails
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtUtil(
    private val secretKey: SecretKey,
    @param:Value("\${jwt.expireMillis}")
    private var expireMillis: Long = 0
) {


    fun parseToken(token: String): Claims =
        Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

    fun getAuthentication(claims: Claims): Authentication {
        val userId = claims.subject.toLong()
        val authorities = claims.get("roles", List::class.java)
            ?.filterIsInstance<String>()
            ?.map(::SimpleGrantedAuthority)
            .orEmpty()

        val user = CustomUserDetails(userId, authorities)

        return UsernamePasswordAuthenticationToken(user, null, authorities)
    }

    fun createToken(userId: Long, roles: List<String>): String {
        val issuedAt = Date()
        val expiration = Date(issuedAt.time + expireMillis)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("roles", roles)
            .issuedAt(issuedAt)
            .expiration(expiration)
            .signWith(secretKey)
            .compact()
    }
}
