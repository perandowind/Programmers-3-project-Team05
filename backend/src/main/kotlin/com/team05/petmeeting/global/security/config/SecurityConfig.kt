package com.team05.petmeeting.global.security.config

import com.team05.petmeeting.global.security.filter.JwtAuthenticationFilter
import com.team05.petmeeting.global.security.filter.InternalSyncAuthenticationFilter
import com.team05.petmeeting.global.security.handler.JwtAccessDeniedHandler
import com.team05.petmeeting.global.security.handler.JwtAuthenticationEntryPoint
import com.team05.petmeeting.global.security.oauth.CustomOAuth2UserService
import com.team05.petmeeting.global.security.oauth.OAuth2SuccessHandler
import com.fasterxml.jackson.databind.ObjectMapper
import com.team05.petmeeting.domain.animal.config.AnimalSyncProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.beans.factory.ObjectProvider

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    private val jwtAccessDeniedHandler: JwtAccessDeniedHandler,
    private val jwtFilter: JwtAuthenticationFilter,
    private val internalSyncAuthenticationFilterProvider: ObjectProvider<InternalSyncAuthenticationFilter>,
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2SuccessHandler: OAuth2SuccessHandler,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            cors { }
            csrf { disable() }
            formLogin { disable() }
            httpBasic { disable() }

            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }

            exceptionHandling {
                authenticationEntryPoint = jwtAuthenticationEntryPoint
                accessDeniedHandler = jwtAccessDeniedHandler
            }

            oauth2Login {
                userInfoEndpoint {
                    userService = customOAuth2UserService
                }
                authenticationSuccessHandler = oAuth2SuccessHandler
            }

            authorizeHttpRequests {
                authorize(HttpMethod.OPTIONS, "/**", permitAll) // CORS Preflight

                authorize("/api/v1/auth/email/**", permitAll)
                authorize("/api/v1/auth/logout", permitAll)
                authorize("/api/v1/auth/refresh", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize("/actuator/**", permitAll) // 메트릭 엔드포인트 허용

                authorize(HttpMethod.GET, "/api/v1/animals", permitAll)
                authorize(HttpMethod.GET, "/api/v1/animals/**", permitAll)
                authorize(HttpMethod.GET, "/api/v1/animals/*/hearts", permitAll)
                authorize(HttpMethod.GET, "/api/v1/feeds", permitAll)
                authorize(HttpMethod.GET, "/api/v1/feeds/**", permitAll)
                authorize(HttpMethod.GET, "/api/v1/shelters/*/campaign", permitAll)
                authorize(HttpMethod.GET, "/api/v1/campaigns", permitAll)
                authorize(HttpMethod.GET, "/api/v1/shelters/*", permitAll)

                authorize(HttpMethod.POST, "/api/v1/animals/sync", hasRole("INTERNAL_SYNC"))
                authorize(HttpMethod.POST, "/api/v1/animals/sync/initial", hasRole("INTERNAL_SYNC"))
                authorize(HttpMethod.POST, "/api/v1/animals/sync/update", hasRole("INTERNAL_SYNC"))
                authorize(HttpMethod.POST, "/api/v1/donations/webhook", permitAll)

                authorize("/error", permitAll) // 처리 못 한 예외 포워딩에 대한 경로 오픈

                authorize("/api/v1/me/**", authenticated)
                authorize(anyRequest, authenticated)
            }

            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtFilter)
        }

        internalSyncAuthenticationFilterProvider.ifAvailable {
            http.addFilterBefore(it, UsernamePasswordAuthenticationFilter::class.java)
        }

        return http.build()
    }

    @Bean
    fun internalSyncAuthenticationFilter(
        animalSyncProperties: AnimalSyncProperties,
        objectMapper: ObjectMapper,
    ): InternalSyncAuthenticationFilter = InternalSyncAuthenticationFilter(animalSyncProperties, objectMapper)

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager
}
