package com.financelog.backend.exception.custom;

import com.financelog.backend.exception.ResponseStatus;

public class AuthException extends RuntimeException{

    private final ResponseStatus responseStatus;

    public AuthException(String message, ResponseStatus status, Throwable cause) {
        super(message, cause);
        this.responseStatus = status;
    }

    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

}
