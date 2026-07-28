package com.team05.petmeeting.global.exception

import org.springframework.http.HttpStatus

interface ErrorCode {

    val status: HttpStatus

    val code: String // frontend가 사용하는 식별자

    val message: String
}
