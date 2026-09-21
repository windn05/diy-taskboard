package com.taskboard.exception;

/** 짧은 시간에 로그인 실패가 반복될 때. 비밀번호 무차별 대입 지연용 */
public class TooManyAttemptsException extends RuntimeException {
    public TooManyAttemptsException(String message) {
        super(message);
    }
}
