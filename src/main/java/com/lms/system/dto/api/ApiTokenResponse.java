package com.lms.system.dto.api;

public record ApiTokenResponse(String tokenType, String accessToken, long expiresIn) {
}
