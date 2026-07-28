package com.team05.petmeeting.global.security.errorcode

import com.team05.petmeeting.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class SecurityErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String

) : ErrorCode {

    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "S-001", "토큰이 만료되었습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "S-002", "유효하지 않은 토큰, 로그인 재시도"),
    UNAUTHORIZED(HttpStatus.FORBIDDEN, "S-003", "접근 권한이 없습니다.");
}
