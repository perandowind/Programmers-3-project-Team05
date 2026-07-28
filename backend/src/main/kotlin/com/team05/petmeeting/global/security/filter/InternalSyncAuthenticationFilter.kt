package com.team05.petmeeting.global.security.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.team05.petmeeting.domain.animal.config.AnimalSyncProperties
import com.team05.petmeeting.domain.animal.errorcode.AnimalErrorCode
import com.team05.petmeeting.global.exception.ErrorResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.security.MessageDigest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class InternalSyncAuthenticationFilter(
    private val animalSyncProperties: AnimalSyncProperties,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (!isAuthorized(request.getHeader(SYNC_SECRET_HEADER))) {
            writeForbidden(response)
            return
        }

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                INTERNAL_SYNC_PRINCIPAL,
                null,
                listOf(SimpleGrantedAuthority(ROLE_INTERNAL_SYNC)),
            )

        filterChain.doFilter(request, response)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.method != "POST" || request.requestURI !in SYNC_PATHS

    private fun isAuthorized(syncSecret: String?): Boolean {
        val configuredSecret = animalSyncProperties.secret
        return syncSecret != null &&
            MessageDigest.isEqual(
                syncSecret.toByteArray(Charsets.UTF_8),
                configuredSecret.toByteArray(Charsets.UTF_8),
            )
    }

    private fun writeForbidden(response: HttpServletResponse) {
        if (response.isCommitted) {
            return
        }

        response.status = AnimalErrorCode.INVALID_SYNC_SECRET.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(
            objectMapper.writeValueAsString(ErrorResponse.from(AnimalErrorCode.INVALID_SYNC_SECRET)),
        )
    }

    companion object {
        const val SYNC_SECRET_HEADER = "X-Internal-Secret"
        const val ROLE_INTERNAL_SYNC = "ROLE_INTERNAL_SYNC"
        private const val INTERNAL_SYNC_PRINCIPAL = "internal-animal-sync"
        private val SYNC_PATHS = setOf(
            "/api/v1/animals/sync",
            "/api/v1/animals/sync/initial",
            "/api/v1/animals/sync/update",
        )
    }
}
