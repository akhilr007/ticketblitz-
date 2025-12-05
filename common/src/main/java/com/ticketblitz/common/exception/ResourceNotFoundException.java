package com.ticketblitz.common.exception;

public class ResourceNotFoundException extends BusinessException{
    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(
                "RESOURCE_NOT_FOUND",
                String.format("%s not found with id: %s", resourceType, identifier),
                404
        );
    }
}