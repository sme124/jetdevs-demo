package com.narola.jetdevstest.exception;

public class CustomException extends RuntimeException {

    private String errMessage;

    public CustomException(String errMessage) {
        super(errMessage);
        this.errMessage = errMessage;
    }
}
