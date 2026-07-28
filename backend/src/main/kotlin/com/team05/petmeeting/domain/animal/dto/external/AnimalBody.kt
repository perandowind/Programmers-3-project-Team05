package com.team05.petmeeting.domain.animal.dto.external

// 실제 데이터 목록과 페이징 메타데이터를 담는 응답 body
data class AnimalBody @JvmOverloads constructor(
    var items: AnimalItems? = null,
    var numOfRows: Int? = null,
    var pageNo: Int? = null,
    var totalCount: Int? = null,
)
