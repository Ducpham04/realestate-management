package com.realestate.common.exception;

public class ConflictException extends AppException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
