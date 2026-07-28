package com.team05.petmeeting.domain.user.service

import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.errorcode.UserErrorCode
import com.team05.petmeeting.domain.user.repository.UserRepository
import com.team05.petmeeting.global.exception.BusinessException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository
) {
    // Todo : 레포지토리 전환 후 elvis 로 throw
    fun findById(id: Long): User = userRepository.findById(id)
        .orElseThrow { BusinessException(UserErrorCode.USER_NOT_FOUND) }
}
