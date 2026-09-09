package com.example.task_flow_backend.common.exception;

/**
 * A domain invariant was violated (e.g. starting a second active sprint,
 * moving an issue to an illegal status). Rendered as HTTP 409.
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
