package com.team05.petmeeting.domain.animal.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "animal.api")
class AnimalApiProperties {
    // 외부 유기동물 API 기본 URL
    var baseUrl: String = ""
    // 외부 API 인증 서비스 키
    var serviceKey: String = ""
    // 외부 API 응답 형식
    var returnType: String = ""
    // 외부 API 연결/응답 타임아웃
    var timeoutMs: Int = 5000
}
