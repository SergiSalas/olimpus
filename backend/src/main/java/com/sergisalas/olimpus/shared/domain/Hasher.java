package com.sergisalas.olimpus.shared.domain;

/**
 * Puerto: convertir un secreto en algo que se puede guardar sin peligro.
 *
 * <p>Ni los codigos del email ni las llaves de sesion se guardan tal cual: si
 * alguien llegara a leer la base de datos, no podria entrar con lo que hay
 * dentro.
 */
public interface Hasher {

    String hash(String secret);
}
