package com.sergisalas.olimpus.matching.domain;

/** Las dos rondas del dia. */
public enum RoundKind {
    /** La de las 4:00: reparte a todo el mundo. */
    PRINCIPAL,
    /**
     * La de las 14:00: solo para quien se quedo sin pareja o cuya conversacion
     * sigue en silencio a mediodia. Evita que quien probo la app un dia se vaya
     * con la sensacion de que no pasa nada.
     */
    REPESCA
}
