package com.team05.petmeeting.global.config

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class QueryDslConfig {
    @PersistenceContext
    private lateinit var em: EntityManager

    @Bean
    fun queryFactory(): JPAQueryFactory {
        return JPAQueryFactory(em)
    }
}
