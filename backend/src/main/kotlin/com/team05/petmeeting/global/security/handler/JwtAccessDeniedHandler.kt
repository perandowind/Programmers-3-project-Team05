package com.team05.petmeeting.global.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.team05.petmeeting.global.exception.ErrorResponse
import com.team05.petmeeting.global.security.errorcode.SecurityErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class JwtAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        response.contentType = "application/json; charset=UTF-8"
        response.status = HttpServletResponse.SC_FORBIDDEN

        val errorResponse = ErrorResponse.from(SecurityErrorCode.UNAUTHORIZED)
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
