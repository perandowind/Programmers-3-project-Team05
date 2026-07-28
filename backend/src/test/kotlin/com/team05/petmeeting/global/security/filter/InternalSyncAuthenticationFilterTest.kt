package com.team05.petmeeting.global.security.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.team05.petmeeting.domain.animal.config.AnimalSyncProperties
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class InternalSyncAuthenticationFilterTest {

    private val properties = AnimalSyncProperties().apply {
        secret = "test-sync-secret"
    }
    private val filter = InternalSyncAuthenticationFilter(properties, ObjectMapper())

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `sync endpoint with valid secret authenticates internal request`() {
        val request = MockHttpServletRequest("POST", "/api/v1/animals/sync").apply {
            addHeader(InternalSyncAuthenticationFilter.SYNC_SECRET_HEADER, "test-sync-secret")
        }
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, _ -> }

        filter.doFilter(request, response, chain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertThat(response.status).isEqualTo(200)
        assertThat(authentication).isNotNull
        assertThat(requireNotNull(authentication).authorities.map { it.authority })
            .containsExactly(InternalSyncAuthenticationFilter.ROLE_INTERNAL_SYNC)
    }

    @Test
    fun `sync endpoint without secret returns forbidden`() {
        val request = MockHttpServletRequest("POST", "/api/v1/animals/sync")
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, _ -> }

        filter.doFilter(request, response, chain)

        assertThat(response.status).isEqualTo(403)
        assertThat(response.contentAsString).contains("A-007")
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    fun `non sync endpoint is skipped`() {
        val request = MockHttpServletRequest("GET", "/api/v1/animals")
        val response = MockHttpServletResponse()
        var called = false
        val chain = FilterChain { _, _ -> called = true }

        filter.doFilter(request, response, chain)

        assertThat(called).isTrue()
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }
}
