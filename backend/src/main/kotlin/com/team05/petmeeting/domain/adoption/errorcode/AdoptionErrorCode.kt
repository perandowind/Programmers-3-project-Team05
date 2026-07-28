package com.team05.petmeeting.domain.adoption.errorcode

import com.team05.petmeeting.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class AdoptionErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "AD-001", "입양 신청 내역이 없습니다."),
    ALREADY_APPLIED(HttpStatus.CONFLICT, "AD-002", "이미 입양 신청한 동물입니다."),
    UNAUTHORIZED_SHELTER(HttpStatus.FORBIDDEN, "AD-003", "해당 보호소의 관리자가 아닙니다."),
    FORBIDDEN_SHELTER_APPLICATION(HttpStatus.FORBIDDEN, "AD-004", "담당 보호소의 입양 신청만 조회할 수 있습니다."),
    INVALID_REVIEW_STATUS(HttpStatus.BAD_REQUEST, "AD-005", "입양 심사 상태가 올바르지 않습니다."),
    REJECTION_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "AD-006", "거절 사유를 입력해야 합니다.");
}
