package com.ticketblitz.common.exception;

public class InvalidCredentialsException extends BusinessException{
    public InvalidCredentialsException() {
        super(
                "INVALID_CREDENTIALS",
                "Invalid email or password",
                401
        );
    }
}