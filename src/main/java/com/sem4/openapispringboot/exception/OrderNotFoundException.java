package com.sem4.openapispringboot.exception;

public class OrderNotFoundException extends RuntimeException {
    private final String code;

    public OrderNotFoundException(String code) {
        super("Không tìm thấy đơn hàng với mã: " + code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
