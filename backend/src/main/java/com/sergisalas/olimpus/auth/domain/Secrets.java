package com.sergisalas.olimpus.auth.domain;

/** Puerto: generar cosas imposibles de adivinar. */
public interface Secrets {

    /** Codigo de seis cifras, con ceros por delante si hace falta. */
    String sixDigitCode();

    /** Llave de sesion larga, la que guarda el movil. */
    String sessionToken();
}
