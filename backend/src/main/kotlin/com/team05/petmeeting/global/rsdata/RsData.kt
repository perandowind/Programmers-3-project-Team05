package com.team05.petmeeting.global.rsdata

data class RsData<T>(
    val msg: String,
    val resultCode: String,
    val data: T? = null
)
