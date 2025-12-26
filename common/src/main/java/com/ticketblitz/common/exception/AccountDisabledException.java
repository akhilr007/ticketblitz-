package com.ticketblitz.common.exception;

public class AccountDisabledException extends BusinessException{
    public AccountDisabledException() {
        super(
                "ACCOUNT_DISABLED",
                "Account is disabled",
                403
        );
    }
}