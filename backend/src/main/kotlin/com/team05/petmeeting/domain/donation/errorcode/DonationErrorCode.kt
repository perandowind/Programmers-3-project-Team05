package com.team05.petmeeting.domain.donation.errorcode

import com.team05.petmeeting.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class DonationErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    DONATION_NOT_FOUND(HttpStatus.NOT_FOUND, "DO-001", "후원 내역이 없습니다."),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "DO-002", "유효하지 않은 금액입니다."),
    ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "DO-003", "이미 처리된 결제입니다."),
    AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "DO-004", "결제 금액이 일치하지 않습니다."),
    PAYMENT_NOT_PAID(HttpStatus.BAD_REQUEST, "DO-005", "결제가 완료되지 않았습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "DO-006", "인증에 실패했습니다."),
    UNKNOWN(HttpStatus.INTERNAL_SERVER_ERROR, "DO-007", "알 수 없는 오류입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "DO-008", "접근 권한이 없습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DO-009", "결제 정보를 찾을 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "DO-010", "잘못된 결제 요청입니다."),
    INVALID_WEBHOOK(HttpStatus.BAD_REQUEST, "DO-011", "유효하지 않은 웹훅입니다."),
    ;
}
