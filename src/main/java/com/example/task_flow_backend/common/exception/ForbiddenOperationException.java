package com.example.task_flow_backend.common.exception;

/**
 * The caller is authenticated but not allowed to act on this particular
 * resource (not a project member, not the comment author, ...). Rendered as 403.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
