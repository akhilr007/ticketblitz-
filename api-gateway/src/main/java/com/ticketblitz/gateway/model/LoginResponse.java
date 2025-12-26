package com.ticketblitz.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private long accessTokenExpiresIn;
    private String tokenType;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
}