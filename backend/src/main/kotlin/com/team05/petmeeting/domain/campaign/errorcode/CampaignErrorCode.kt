package com.team05.petmeeting.domain.campaign.errorcode

import com.team05.petmeeting.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class CampaignErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    CAMPAIGN_NOT_FOUND(HttpStatus.NOT_FOUND, "CA-001", "캠페인이 존재하지 않습니다."),
    CAMPAIGN_CLOSED(HttpStatus.BAD_REQUEST, "CA-002", "해당 캠페인은 이미 마감됐습니다."),
    CAMPAIGN_ALREADY_EXISTS(HttpStatus.CONFLICT, "CA-003", "이미 진행 중인 캠페인이 있습니다."),
    UNAUTHORIZED_SHELTER(HttpStatus.FORBIDDEN, "CA-004", "해당 보호소의 관리자가 아닙니다."),
    ;
}
