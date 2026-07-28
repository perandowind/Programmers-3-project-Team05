package com.team05.petmeeting.domain.feed.errorcode

import com.team05.petmeeting.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class FeedErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    INVALID_PAGE(HttpStatus.BAD_REQUEST, "F-001", "잘못된 페이지 번호입니다."),
    FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "F-002", "존재하지 않거나 이미 삭제된 피드입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "F-003", "로그인이 필요한 서비스입니다."),
    LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "F-004", "피드는 10~1000자 범위에서 작성해야합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "F-005", "권한이 없습니다."),  // 삭제, 수정
    ANIMAL_REQUIRED(HttpStatus.BAD_REQUEST, "F-006", "입양후기는 동물 선택이 필수입니다."),
    NOT_ADOPTED_ANIMAL(HttpStatus.FORBIDDEN, "F-007", "입양 승인된 동물만 후기를 작성할 수 있습니다.");
}
