package com.realestate.common.exception;

/** Thrown when a request is well-formed but violates a domain rule. */
public class BusinessException extends AppException {

    public BusinessException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }
}
