package com.taskboard.exception;

/** 권한 없음 → 403. Spring Security의 동명 클래스와 혼동하지 않도록 import 주의 */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
