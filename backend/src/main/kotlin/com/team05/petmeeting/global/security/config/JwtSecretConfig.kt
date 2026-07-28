package com.team05.petmeeting.global.security.config

import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey

@Configuration
class JwtSecretConfig(
    @param:Value("\${jwt.secret}")
    private val secret: String,
) {

    @Bean
    fun secretKey(): SecretKey =
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
}
