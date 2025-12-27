package com.ticketblitz.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshResult {
    private RefreshTokenResponse refreshTokenResponse;
    private String refreshToken;
}