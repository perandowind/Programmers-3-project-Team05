package com.team05.petmeeting.domain.animal.dto;

public record AnimalSyncRes(
        String message,
        int savedCount,
        long elapsedMs
) {
}
