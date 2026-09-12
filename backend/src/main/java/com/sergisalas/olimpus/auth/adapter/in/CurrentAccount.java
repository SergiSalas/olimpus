package com.sergisalas.olimpus.auth.adapter.in;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un parametro que debe rellenarse con la cuenta de quien llama.
 * Si no hay sesion valida, la peticion se corta con un 401.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentAccount {}
