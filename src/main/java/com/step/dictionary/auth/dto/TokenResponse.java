package com.step.dictionary.auth.dto;

/**
 * 로그인 성공 응답.
 *
 * <pre>
 * {"accessToken": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer", "expiresIn": 3600}
 * </pre>
 *
 * @param accessToken JWT 액세스 토큰. 이후 요청에 {@code Authorization: Bearer <accessToken>}으로 보낸다
 * @param tokenType   토큰 타입. 항상 {@code Bearer}
 * @param expiresIn   유효 시간(초). 만료되면 다시 로그인한다 (리프레시 토큰은 없음)
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
