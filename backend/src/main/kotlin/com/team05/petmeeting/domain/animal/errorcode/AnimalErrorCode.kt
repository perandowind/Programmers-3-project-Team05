package com.team05.petmeeting.domain.animal.errorcode

import com.team05.petmeeting.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class AnimalErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    // HttpStatus 객체 사용 및 코드에 식별자(A-) 부여
    ANIMAL_NOT_FOUND(HttpStatus.NOT_FOUND, "A-001", "존재하지 않는 유기동물입니다."),
    INVALID_PAGE_NUMBER(HttpStatus.BAD_REQUEST, "A-002", "잘못된 페이지 번호입니다"),
    INVALID_SYNC_REQUEST(HttpStatus.BAD_REQUEST, "A-003", "동기화 요청 값이 올바르지 않습니다."),
    ANIMAL_SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A-004", "유기동물 데이터 적재 중 오류가 발생했습니다."),
    INITIAL_ANIMAL_SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A-005", "유기동물 최초 적재 중 오류가 발생했습니다."),
    UPDATE_ANIMAL_SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A-006", "유기동물 업데이트 적재 중 오류가 발생했습니다."),
    INVALID_SYNC_SECRET(HttpStatus.FORBIDDEN, "A-007", "동기화 요청 권한이 없습니다."),
    ;
}
