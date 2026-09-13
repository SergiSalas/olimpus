package com.sergisalas.olimpus.chat.domain;

/** Alguien intenta leer o escribir en una conversacion que no es suya. */
public class NotYourConversationException extends RuntimeException {

    public NotYourConversationException() {
        super("Esa conversación no es tuya.");
    }
}
