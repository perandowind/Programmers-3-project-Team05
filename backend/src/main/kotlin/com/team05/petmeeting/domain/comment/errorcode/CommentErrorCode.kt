package com.team05.petmeeting.domain.comment.errorcode

import com.team05.petmeeting.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class CommentErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    // HttpStatus 객체 사용 및 코드에 식별자(C-) 부여
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C-001", "로그인이 필요한 서비스입니다."),
    FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "C-002", "존재하지 않거나 이미 삭제된 피드입니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "C-003", "존재하지 않거나 이미 삭제된 댓글입니다."),
    ;
}
