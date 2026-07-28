package com.team05.petmeeting.domain.naming.service

import com.team05.petmeeting.domain.naming.repository.BadWordRepository
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BadWordWarmupRunner(
    private val badWordRepository: BadWordRepository,
    private val stringRedisTemplate: StringRedisTemplate
) {
    companion object {
        private const val BAD_WORD_KEY = "naming:badwords"
    }

    @EventListener(ApplicationReadyEvent::class)
    @Transactional(readOnly = true)
    fun warmup() {
        val words = badWordRepository.findAll().map { it.word }
        if (words.isNotEmpty()) {
            // 초기화 시 기존 키 삭제 후 일괄 삽입
            stringRedisTemplate.delete(BAD_WORD_KEY)
            stringRedisTemplate.opsForSet().add(BAD_WORD_KEY, *words.toTypedArray())
        }
    }
}