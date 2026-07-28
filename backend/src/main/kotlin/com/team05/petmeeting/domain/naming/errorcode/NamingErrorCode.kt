package com.team05.petmeeting.domain.naming.errorcode

import com.team05.petmeeting.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class NamingErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    // HttpStatus 객체 사용 및 코드에 식별자(N-) 부여
    BAD_WORD_INCLUDED(HttpStatus.BAD_REQUEST, "N-001", "이름에 부적절한 단어가 포함되어 있습니다."),
    ALREADY_VOTED(HttpStatus.CONFLICT, "N-002", "이미 이 동물의 이름 투표에 참여하셨습니다."),
    CANDIDATE_NOT_FOUND(HttpStatus.NOT_FOUND, "N-003", "존재하지 않는 이름 후보입니다."),
    VOTING_CLOSED(HttpStatus.BAD_REQUEST, "N-004", "이미 이름이 확정되어 투표가 종료되었습니다."),
    ALREADY_HAS_NAME(HttpStatus.BAD_REQUEST, "N-005", "이미 이름이 확정된 동물입니다."),
    ALREADY_COMPLETED_ANIMAL(HttpStatus.BAD_REQUEST, "N-006", "보호가 종료된 동물입니다."),
    BAD_WORD_NOT_FOUND(HttpStatus.NOT_FOUND, "N-007", "존재하지 않는 금칙어입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "N-008", "이름 결정 권한이 없습니다."),
    ;
}
