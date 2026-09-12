package com.sergisalas.olimpus.auth.domain;

/**
 * Puerto: llevarle el codigo a su dueno.
 *
 * <p>Es una de las piezas intercambiables del proyecto. Mientras se programa,
 * el codigo se escribe en la consola; el dia que haya presupuesto se enchufa
 * un servicio de correo de verdad sin tocar nada mas.
 */
public interface CodeSender {

    void send(EmailAddress email, String code);
}
