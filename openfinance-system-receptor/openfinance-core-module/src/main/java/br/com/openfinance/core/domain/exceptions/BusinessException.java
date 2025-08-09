package br.com.openfinance.core.domain.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final HttpStatus httpStatus;

    public BusinessException(String message, String code) {
        super(message);
        this.code = code;
        this.httpStatus = HttpStatus.UNPROCESSABLE_ENTITY;
    }

    public BusinessException(String message, String code, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public BusinessException(String message, String code, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
