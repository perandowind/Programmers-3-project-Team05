package com.team05.petmeeting.domain.naming.service

import com.team05.petmeeting.domain.naming.entity.BadWord
import com.team05.petmeeting.domain.naming.errorcode.NamingErrorCode
import com.team05.petmeeting.domain.naming.repository.BadWordRepository
import com.team05.petmeeting.global.exception.BusinessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.Long
import kotlin.String

@Service
@Transactional
// Redis 기반 금칙어 검증 로직
class BadWordService(
    private val badWordRepository: BadWordRepository,
    // 기존 RedisConfig에 정의된 빈 사용
    private val stringRedisTemplate: StringRedisTemplate
) {
    companion object {
        private const val BAD_WORD_KEY = "naming:badwords"
    }

/** StringRedisTemplate은 내부적으로 StringSerializer를 사용하므로
    별도의 직렬화 설정 없이 바로 사용 가능하다.
    opsForSet(): Redis의 Set 자료구조를 사용
    add(Key, Value): 해당 Key(주머니)에 단어를 추가 */


    // 1. 등록: DB 저장과 Redis 추가를 한 번에 처리
    fun register(word: String): BadWord {
        val badWord = badWordRepository.save(BadWord(word))
        stringRedisTemplate.opsForSet().add(BAD_WORD_KEY, word)
        return badWord
    }

    // 2. 삭제: DB 삭제와 Redis 제거를 한 번에 처리
    fun remove(id: Long) {
        val badWord = findById(id) // 내부 findById 활용
        badWordRepository.delete(badWord)
        stringRedisTemplate.opsForSet().remove(BAD_WORD_KEY, badWord.word)
    }

    // 3. 검증: Redis에 없으면 DB 확인 후 Redis 채우기 (방어 로직)
    fun isBadWord(word: String): Boolean {
        // Redis에 키 자체가 존재하는지 먼저 확인
        val hasKey = stringRedisTemplate.hasKey(BAD_WORD_KEY)

        if (hasKey == false) {
            // Redis가 비어있다면 DB에서 복구 (Warm-up과 동일 로직)
            val allWords = badWordRepository.findAll().map { it.word }
            if (allWords.isNotEmpty()) {
                stringRedisTemplate.opsForSet().add(BAD_WORD_KEY, *allWords.toTypedArray())
            }
        }

        return stringRedisTemplate.opsForSet().isMember(BAD_WORD_KEY, word) == true
    }

    fun findById(id: Long): BadWord = badWordRepository.findById(id)
        .orElseThrow { BusinessException(NamingErrorCode.BAD_WORD_NOT_FOUND) }

    fun findAll(): List<BadWord> = badWordRepository.findAll()

}
