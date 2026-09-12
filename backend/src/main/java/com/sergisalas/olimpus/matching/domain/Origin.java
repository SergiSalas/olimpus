package com.sergisalas.olimpus.matching.domain;

/**
 * De donde sale una pareja. Se guarda con cada conversacion, y es lo que
 * permitira responder a la pregunta importante: ¿las parejas que elige el
 * algoritmo funcionan mejor que las del azar?
 */
public enum Origin {
    /** La mejor pareja disponible segun la puntuacion. */
    MEJOR_PAREJA,
    /** Buena, pero no la primera: sirve para no encerrar a nadie en lo de siempre. */
    DESCUBRIMIENTO,
    /** Al azar entre quienes pasan los filtros. Es la referencia a batir. */
    AZAR
}
