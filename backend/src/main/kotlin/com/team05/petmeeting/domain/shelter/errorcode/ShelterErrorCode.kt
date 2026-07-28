package com.team05.petmeeting.domain.shelter.errorcode

import com.team05.petmeeting.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class ShelterErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    SHELTER_NOT_FOUND(HttpStatus.NOT_FOUND, "SH-001", "보호소가 존재하지 않습니다."),
    ;
}
