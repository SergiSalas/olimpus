package com.sergisalas.olimpus.profile.domain;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Lo que alguien declara en el registro: las diez preguntas.
 *
 * <p>Es solo el punto de partida. Con el tiempo debe pesar mas como se
 * comporta de verdad (con quien sigue hablando, con quien conecta) que lo que
 * dijo el primer dia.
 *
 * <p>Las reglas viven aqui, en el constructor: un perfil que exista es un
 * perfil valido. Nadie puede guardar uno a medias por otra puerta.
 */
public record Profile(
        UUID accountId,
        String nickname,
        String bio,
        LocalDate birthDate,
        Gender gender,
        Set<Gender> seeking,
        int ageMin,
        int ageMax,
        int maxDistanceKm,
        Location location,
        List<LanguageSkill> languages,
        /** 1 = me cuesta arrancar, 5 = hablo con cualquiera. */
        int sociability,
        /** 1 = ligera y divertida, 5 = de las que van hondo. */
        int conversationDepth,
        Intent intent,
        Set<String> interests) {

    public static final int EDAD_MINIMA = 18;
    public static final int EDAD_MAXIMA = 99;
    public static final int INTERESES_MIN = 5;
    public static final int INTERESES_MAX = 8;
    public static final int MAX_IDIOMAS = 5;
    public static final int APODO_MIN = 2;
    public static final int APODO_MAX = 20;
    public static final int BIO_MAX = 200;

    /** Las unicas distancias que se pueden elegir, para no inventar numeros raros. */
    public static final List<Integer> DISTANCIAS = List.of(5, 15, 30, 50);

    public Profile {
        if (accountId == null) throw new IllegalArgumentException("falta la cuenta");

        nickname = nickname == null ? "" : nickname.trim();
        if (nickname.length() < APODO_MIN || nickname.length() > APODO_MAX) {
            throw new IllegalArgumentException(
                    "el apodo tiene que tener entre " + APODO_MIN + " y " + APODO_MAX + " letras");
        }

        bio = bio == null ? "" : bio.trim();
        if (bio.length() > BIO_MAX) {
            throw new IllegalArgumentException("la bio no puede pasar de " + BIO_MAX + " caracteres");
        }

        if (birthDate == null) throw new IllegalArgumentException("falta la fecha de nacimiento");
        if (gender == null) throw new IllegalArgumentException("falta tu genero");

        if (seeking == null || seeking.isEmpty()) {
            throw new IllegalArgumentException("hay que elegir al menos un genero que buscas");
        }
        seeking = Set.copyOf(seeking);

        if (ageMin < EDAD_MINIMA || ageMax > EDAD_MAXIMA) {
            throw new IllegalArgumentException(
                    "el rango de edad va de " + EDAD_MINIMA + " a " + EDAD_MAXIMA);
        }
        if (ageMin > ageMax) {
            throw new IllegalArgumentException("el rango de edad esta al reves");
        }

        if (!DISTANCIAS.contains(maxDistanceKm)) {
            throw new IllegalArgumentException("la distancia tiene que ser una de " + DISTANCIAS);
        }

        if (location == null) throw new IllegalArgumentException("falta la ubicacion");

        if (languages == null || languages.isEmpty()) {
            throw new IllegalArgumentException("hay que poner al menos un idioma");
        }
        if (languages.size() > MAX_IDIOMAS) {
            throw new IllegalArgumentException("como mucho " + MAX_IDIOMAS + " idiomas");
        }
        languages = List.copyOf(languages);
        if (languages.stream().map(LanguageSkill::code).distinct().count() != languages.size()) {
            throw new IllegalArgumentException("hay un idioma repetido");
        }

        if (sociability < 1 || sociability > 5) {
            throw new IllegalArgumentException("la escala va de 1 a 5");
        }
        if (conversationDepth < 1 || conversationDepth > 5) {
            throw new IllegalArgumentException("la escala va de 1 a 5");
        }

        if (intent == null) throw new IllegalArgumentException("falta que buscas");

        if (interests == null
                || interests.size() < INTERESES_MIN
                || interests.size() > INTERESES_MAX) {
            throw new IllegalArgumentException(
                    "hay que elegir entre " + INTERESES_MIN + " y " + INTERESES_MAX + " intereses");
        }
        interests = Set.copyOf(interests);
        for (String interest : interests) {
            if (!InterestCatalog.contains(interest)) {
                throw new IllegalArgumentException("ese interes no esta en la lista: " + interest);
            }
        }
    }

    public int ageOn(LocalDate today) {
        return Period.between(birthDate, today).getYears();
    }

    /**
     * Filtro que nunca cede. Se comprueba al guardar el perfil y no depende de
     * que el movil diga la verdad, porque se calcula desde la fecha de
     * nacimiento y el reloj del servidor.
     */
    public boolean isMinorOn(LocalDate today) {
        return ageOn(today) < EDAD_MINIMA;
    }

    /** La escala de 1 a 5 que el algoritmo necesita entre 0 y 1. */
    public double sociabilityScore() {
        return (sociability - 1) / 4.0;
    }

    public double conversationDepthScore() {
        return (conversationDepth - 1) / 4.0;
    }
}
