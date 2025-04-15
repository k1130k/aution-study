package com.fifteen.auction.global.controller;

import com.fifteen.auction.global.dto.error.ErrorCode;
import com.fifteen.auction.global.dto.error.ErrorResponse;
import com.fifteen.auction.global.dto.exception.ClientException;
import com.fifteen.auction.global.dto.exception.ServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClientException.class)
    public ResponseEntity<ErrorResponse> handleClientException(ClientException ce) {
        return ResponseEntity.status(ce.getErrorCode().getStatus())
                .body(ErrorResponse.ofErrorCode(ce.getErrorCode()));
    }

    @ExceptionHandler(ServerException.class)
    public ResponseEntity<ErrorResponse> handleServerException(ServerException se) {
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.ofErrorCode(se.getErrorCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {

        log.error("알 수 없는 예외 발생", e);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.ofErrorCode(ErrorCode.EXCEPTION));
    }
}
