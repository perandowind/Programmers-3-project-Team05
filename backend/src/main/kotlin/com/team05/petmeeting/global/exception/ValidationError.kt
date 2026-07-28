package com.team05.petmeeting.global.exception

data class ValidationError(
    val field: String,
    val reason: String
) 