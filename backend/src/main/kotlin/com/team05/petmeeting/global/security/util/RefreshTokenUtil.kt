package com.team05.petmeeting.global.security.util

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component

@Component
class RefreshTokenUtil {

    fun add(response: HttpServletResponse, refreshToken: String) {
        Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken).apply {
            isHttpOnly = true
            secure = true
            setAttribute("SameSite", "None")
            path = "/"
            maxAge = REFRESH_TOKEN_MAX_AGE
        }.also(response::addCookie)
    }

    fun delete(response: HttpServletResponse) {
        Cookie(REFRESH_TOKEN_COOKIE_NAME, null).apply {
            isHttpOnly = true
            secure = true
            path = "/"
            maxAge = 0
        }.also(response::addCookie)
    }

    companion object {
        const val REFRESH_TOKEN_COOKIE_NAME = "refreshToken"
        private const val REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60
    }
}
