package com.team05.petmeeting.global.exception

import org.springframework.http.HttpStatus

enum class GlobalErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "G-001", "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G-004", "서버 내부 오류가 발생했습니다.");
}