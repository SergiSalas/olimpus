package com.sergisalas.olimpus.chat.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Un mensaje dentro de la conversacion del dia.
 *
 * <p>Texto y nada mas: en la primera version no hay fotos en el chat, porque la
 * foto es justo lo que la app se guarda para el nivel 3.
 */
public record Message(
        UUID id, UUID conversationId, UUID senderAccountId, String text, Instant sentAt) {

    public static final int MAX_LARGO = 1000;

    public Message {
        if (id == null) throw new IllegalArgumentException("falta el id del mensaje");
        if (conversationId == null) throw new IllegalArgumentException("falta la conversacion");
        if (senderAccountId == null) throw new IllegalArgumentException("falta quien lo escribe");
        if (sentAt == null) throw new IllegalArgumentException("falta la hora");

        text = text == null ? "" : text.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("el mensaje esta vacio");
        }
        if (text.length() > MAX_LARGO) {
            throw new IllegalArgumentException(
                    "el mensaje no puede pasar de " + MAX_LARGO + " caracteres");
        }
    }

    public static Message written(UUID conversationId, UUID sender, String text, Instant now) {
        return new Message(UUID.randomUUID(), conversationId, sender, text, now);
    }
}
