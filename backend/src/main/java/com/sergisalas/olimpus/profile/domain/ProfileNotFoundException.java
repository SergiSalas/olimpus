package com.sergisalas.olimpus.profile.domain;

/** Todavia no hay registro: la app manda a la persona a hacerlo. */
public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException() {
        super("Todavía no has hecho el registro.");
    }
}
