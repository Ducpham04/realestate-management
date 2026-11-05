package com.realestate.common.exception;

public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(String resource, Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, "%s not found with id %s".formatted(resource, id));
    }
}
