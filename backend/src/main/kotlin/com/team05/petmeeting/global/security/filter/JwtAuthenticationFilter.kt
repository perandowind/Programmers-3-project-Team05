package com.team05.petmeeting.global.security.filter

import com.team05.petmeeting.global.security.handler.JwtAuthenticationEntryPoint
import com.team05.petmeeting.global.security.util.JwtUtil
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil,
    private val entryPoint: JwtAuthenticationEntryPoint,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val token = resolveToken(request)

            if (token == null || SecurityContextHolder.getContext().authentication != null) {
                filterChain.doFilter(request, response)
                return
            }

            val claims = jwtUtil.parseToken(token)
            SecurityContextHolder.getContext().authentication = jwtUtil.getAuthentication(claims)
        } catch (ex: ExpiredJwtException) {
            entryPoint.commence(request, response, CredentialsExpiredException("토큰 만료"))
            return
        } catch (ex: JwtException) {
            entryPoint.commence(request, response, BadCredentialsException("유효하지 않은 토큰"))
            return
        }

        filterChain.doFilter(request, response)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI.let { path ->
            path.contains("/logout") || path.contains("/refresh")
        }

    private fun resolveToken(request: HttpServletRequest): String? =
        request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.replace("Bearer ", "")

    override fun shouldNotFilterAsyncDispatch(): Boolean = false
}
