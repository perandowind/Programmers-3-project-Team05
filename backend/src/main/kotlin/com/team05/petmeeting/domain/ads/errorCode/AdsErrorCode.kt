package com.team05.petmeeting.domain.ads.errorCode

import com.team05.petmeeting.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class AdsErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    POST_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "ADS-001", "광고 게시 요청이 없습니다."),
    ANIMAL_SHELTER_NOT_FOUND(HttpStatus.BAD_REQUEST, "ADS-002", "동물의 보호소 정보가 없습니다."),
    UNAUTHORIZED_SHELTER(HttpStatus.FORBIDDEN, "ADS-003", "해당 보호소의 관리자가 아닙니다."),
    FORBIDDEN_SHELTER_REQUEST(HttpStatus.FORBIDDEN, "ADS-004", "담당 보호소의 광고 게시 요청만 처리할 수 있습니다."),
    INVALID_REVIEW_STATUS(HttpStatus.BAD_REQUEST, "ADS-005", "광고 게시 심사 상태가 올바르지 않습니다."),
    REJECTION_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "ADS-006", "거절 사유를 입력해야 합니다."),
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "ADS-007", "이미 처리된 광고 게시 요청입니다.");
}
