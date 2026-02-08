package com.financelog.backend.dto;

import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

public class APIResponse<T> implements Serializable {

    private final String status;
    private final int statusCode;
    private final String message;
    private final LocalDateTime time;
    private final T data;

    private APIResponse(HttpStatus httpStatus, String message, T data) {
        this.status = httpStatus.name();
        this.statusCode = httpStatus.value();
        this.message = message;
        this.time = LocalDateTime.now();
        this.data = data;
    }

    /**
     * Standard Success Response (200 OK)
     */
    public static <T> APIResponse<T> ok(T data, String message) {
        return new APIResponse<>(HttpStatus.OK, message, data);
    }

    /**
     * Standard Created Response (201 CREATED)
     */
    public static <T> APIResponse<T> created(T data, String message) {
        return new APIResponse<>(HttpStatus.CREATED, message, data);
    }

    /**
     * Generic Response for any other Status (Accepted, No Content, etc.)
     */
    public static <T> APIResponse<T> of(HttpStatus status, T data, String message) {
        return new APIResponse<>(status, message, data);
    }

    public String getStatus() { return status; }
    public int getStatusCode() { return statusCode; }
    public String getMessage() { return message; }
    public LocalDateTime getTime() { return time; }
    public T getData() { return data; }
}
