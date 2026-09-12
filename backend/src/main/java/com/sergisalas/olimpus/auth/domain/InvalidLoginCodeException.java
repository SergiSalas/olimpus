package com.sergisalas.olimpus.auth.domain;

/**
 * El codigo no vale: no existe, ha caducado, no coincide o se han agotado los
 * intentos. A quien lo escribe se le dice siempre lo mismo, para no dar pistas
 * a quien este probando codigos a ciegas.
 */
public class InvalidLoginCodeException extends RuntimeException {

    public InvalidLoginCodeException(String motivo) {
        super(motivo);
    }
}
