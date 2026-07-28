package com.team05.petmeeting.domain.cheer.errorcode

import com.team05.petmeeting.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class CheerErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    // HttpStatus 객체 사용 및 코드에 식별자(CH-) 부여
    DAILY_CHEER_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "CH-001", "오늘의 응원 하트를 모두 사용했습니다.")
    ;
}
