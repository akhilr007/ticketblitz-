package com.ticketblitz.common.exception;


import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;
    private final Object details;

    public BusinessException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = null;
    }

    public BusinessException(String errorCode, String message, int httpStatus, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = details;
    }
}