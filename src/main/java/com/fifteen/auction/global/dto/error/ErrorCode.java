package com.fifteen.auction.global.dto.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /**
     * 여기에 에러 코드를 추가하세요
     * '// user 에러 코드' 와 같은 주석으로 도메인을 분리해주시면 됩니다.
     * 상수를 선언하실 때, code 부분은 '도메인-숫자'의 형식으로 해주세요. 예) USER-1
     */

    // Uncaught Exceptions
    EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "EXCEPTION", "알 수 없는 에러입니다."),
    MARKET_PRICE_NOT_FOUND(HttpStatus.NOT_FOUND, "MARKETPRICE-1", "시세 정보가 존재하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
