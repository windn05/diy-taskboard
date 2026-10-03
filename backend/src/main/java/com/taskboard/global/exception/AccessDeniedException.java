package com.taskboard.global.exception;

/** 권한 없음 (403) */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
