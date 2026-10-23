package com.sem4.openapispringboot.exception;

public class BusinessValidationException extends RuntimeException {
    private final String code;

    public BusinessValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
