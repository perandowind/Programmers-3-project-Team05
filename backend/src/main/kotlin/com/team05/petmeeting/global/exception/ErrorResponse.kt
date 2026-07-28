package com.team05.petmeeting.global.exception

import com.fasterxml.jackson.annotation.JsonInclude

data class ErrorResponse(
    val code: String,
    val message: String,
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val details: List<ValidationError>?
) {
    companion object {
        @JvmStatic
        fun from(errorCode: ErrorCode): ErrorResponse {
            return ErrorResponse(
                errorCode.code,
                errorCode.message,
                null
            )
        }

        // valid 검증 실패 에러는 details 포함
        @JvmStatic
        fun of(code: String, message: String, details: List<ValidationError>): ErrorResponse {
            return ErrorResponse(code, message, details)
        }
    }
}
