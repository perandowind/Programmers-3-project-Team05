package com.team05.petmeeting.domain.animal.dto.external

// 외부 유기동물 API가 반환하는 개별 동물 정보
data class AnimalItem @JvmOverloads constructor(
    var desertionNo: String? = null,
    var processState: String? = null,
    var noticeNo: String? = null,
    var noticeEdt: String? = null,
    var happenPlace: String? = null,
    var upKindNm: String? = null,
    var kindFullNm: String? = null,
    var colorCd: String? = null,
    var age: String? = null,
    var weight: String? = null,
    var sexCd: String? = null,
    var popfile1: String? = null,
    var popfile2: String? = null,
    var specialMark: String? = null,
    var careOwnerNm: String? = null,
    var careNm: String? = null,
    var careAddr: String? = null,
    var careTel: String? = null,
    var updTm: String? = null,
    var careRegNo: String? = null,
    var orgNm: String? = null,
)
