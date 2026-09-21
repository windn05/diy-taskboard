package com.taskboard.exception;

/** 대상 없음 → 404 */
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
