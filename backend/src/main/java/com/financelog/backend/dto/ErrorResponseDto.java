package com.financelog.backend.dto;

import com.financelog.backend.exception.ResponseStatus;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public class ErrorResponseDto {

    private final String apiPath;
    private final int statusCode;
    private final HttpStatus errorStatus;
    private final String errorCode;
    private final LocalDateTime errorTime;
    private final String causeMsg;

    public ErrorResponseDto(String apiPath, ResponseStatus responseStatus, String causeMsg){
        this.apiPath = apiPath;
        this.statusCode = responseStatus.getStatusCode();
        this.errorStatus = responseStatus.getStatusMsg();
        this.errorCode = responseStatus.name();
        this.errorTime = LocalDateTime.now();
        this.causeMsg = causeMsg;
    }

    public String getApiPath() {
        return apiPath;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public HttpStatus getErrorStatus() {
        return errorStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public LocalDateTime getErrorTime() {
        return errorTime;
    }

    public String getCauseMsg() {
        return causeMsg;
    }
}
