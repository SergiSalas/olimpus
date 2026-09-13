package com.sergisalas.olimpus.chat.domain;

/** Ya son las 22:00, o la conversacion se cancelo: no se puede escribir mas. */
public class ChatClosedException extends RuntimeException {

    public ChatClosedException(String motivo) {
        super(motivo);
    }
}
