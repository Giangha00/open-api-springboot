package com.sem4.openapispringboot.exception;

public class InvalidTransitionException extends RuntimeException {
    private final String orderCode;
    private final String fromStatus;
    private final String action;

    public InvalidTransitionException(String orderCode, String fromStatus, String action, String message) {
        super(message);
        this.orderCode = orderCode;
        this.fromStatus = fromStatus;
        this.action = action;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public String getAction() {
        return action;
    }
}
