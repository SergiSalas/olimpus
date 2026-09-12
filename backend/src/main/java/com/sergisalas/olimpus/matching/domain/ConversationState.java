package com.sergisalas.olimpus.matching.domain;

public enum ConversationState {
    /** Viva: se puede escribir hasta la hora de cierre. */
    ABIERTA,
    /** Se cerro en silencio a mediodia y los dos volvieron al reparto. */
    CANCELADA,
    /** Llego las 22:00. A partir de aqui manda la decision de cada uno. */
    CERRADA
}
