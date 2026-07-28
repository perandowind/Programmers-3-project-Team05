package com.team05.petmeeting.global.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.team05.petmeeting.domain.user.errorcode.UserErrorCode
import com.team05.petmeeting.global.exception.ErrorResponse
import com.team05.petmeeting.global.security.errorcode.SecurityErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.contentType = "application/json; charset=UTF-8"
        response.status = HttpServletResponse.SC_UNAUTHORIZED

        val errorCode = when (authException) {
            is CredentialsExpiredException -> SecurityErrorCode.TOKEN_EXPIRED
            is BadCredentialsException -> SecurityErrorCode.INVALID_TOKEN
            else -> {
                log.error("===================={}====================", authException.message)
                UserErrorCode.UNAUTHORIZED
            }
        }

        val errorResponse = ErrorResponse.from(errorCode)
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }

    companion object {
        private val log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint::class.java)
    }
}
