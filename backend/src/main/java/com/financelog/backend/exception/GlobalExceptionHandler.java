package com.financelog.backend.exception;

import com.financelog.backend.dto.ErrorResponseDto;
import com.financelog.backend.exception.custom.AuthException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthException(AuthException cause, WebRequest request){
        return ResponseEntity
                .status(cause.getResponseStatus().getStatusMsg())
                .body(new ErrorResponseDto(
                        request.getDescription(false),
                        cause.getResponseStatus(),
                        cause.getMessage()
                ));
    }
}
