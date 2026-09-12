package com.sergisalas.olimpus.auth.adapter.in;

public class NotAuthenticatedException extends RuntimeException {

    public NotAuthenticatedException(String message) {
        super(message);
    }
}
