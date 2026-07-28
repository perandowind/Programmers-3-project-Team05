package com.team05.petmeeting.global.exception

class BusinessException @JvmOverloads constructor(
    val errorCode: ErrorCode,
    cause: Throwable? = null
) : RuntimeException(errorCode.message, cause)
