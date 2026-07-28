package com.team05.petmeeting.domain.user.errorcode

import com.team05.petmeeting.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class UserErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "U-001", "로그인이 필요한 서비스입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "U-002", "로그인에 실패했습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U-004", "존재하지 않는 사용자입니다."),
    LOCAL_NOT_FOUND(HttpStatus.NOT_FOUND, "U-014", "소셜 로그인으로 가입한 계정입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U-005", "비밀번호가 일치하지 않습니다."),
    SAME_AS_OLD_PASSWORD(HttpStatus.BAD_REQUEST, "U-006", "예전 비밀번호와 같습니다."),
    DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "U-007", "이미 사용 중인 닉네임입니다."),
    TOO_MANY_OTP_REQUEST(HttpStatus.BAD_REQUEST, "U-008", "코드 재발급 요청이 너무 잦습니다. 1분에 1번만 가능합니다."),
    EXPIRED_OTP(HttpStatus.BAD_REQUEST, "U-009", "만료된 코드입니다. 코드를 재발급해주세요."),
    TOO_MANY_OTP_ATTEMPTS(HttpStatus.BAD_REQUEST, "U-010", "코드 확인 요청이 잦습니다. 코드를 재발급 후 시도해주세요."),
    INVALID_OTP(HttpStatus.BAD_REQUEST, "U-011", "잘못된 코드입니다. 코드를 다시 확인해주세요."),
    INVALID_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "U-012", "잘못된 검증 코드입니다."),
    ALREADY_REGISTERED_EMAIL(HttpStatus.BAD_REQUEST, "U-013", "이미 가입된 사용자입니다.");
}
