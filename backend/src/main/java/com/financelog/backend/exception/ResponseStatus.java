package com.financelog.backend.exception;

import org.springframework.http.HttpStatus;

public enum ResponseStatus {

    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED),
    AUTH_USER_NOT_CONFIRMED(HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN),
    AUTH_USER_NOT_FOUND(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND),
    AUTH_CODE_MISMATCH(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST),
    AUTH_CODE_EXPIRED(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST),
    AUTH_USER_ALREADY_EXISTS(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT),
    AUTH_PASSWORD_POLICY_VIOLATED(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST),
    AUTH_UNKNOWN(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST);

    private final int statusCode;
    private final HttpStatus statusMsg;

    ResponseStatus(int statusCode, HttpStatus statusMsg) {
        this.statusCode = statusCode;
        this.statusMsg = statusMsg;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public HttpStatus getStatusMsg() {
        return statusMsg;
    }
}
