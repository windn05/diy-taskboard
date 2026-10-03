package com.taskboard.auth;

/** 로그인 실패가 짧은 시간에 반복될 때 발생 (429) */
public class TooManyAttemptsException extends RuntimeException {
    public TooManyAttemptsException(String message) {
        super(message);
    }
}
