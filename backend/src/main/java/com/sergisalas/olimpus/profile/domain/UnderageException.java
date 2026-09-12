package com.sergisalas.olimpus.profile.domain;

/** La app es solo para mayores de 18. No es una preferencia: es la puerta. */
public class UnderageException extends RuntimeException {

    public UnderageException() {
        super("Olimpus es solo para mayores de 18 años.");
    }
}
