package com.sergisalas.olimpus.profile.domain;

/**
 * Un idioma y cuanto se maneja. El nivel se pregunta con palabras y el
 * algoritmo lo usa como numero entre 0 y 1.
 */
public record LanguageSkill(String code, Level level) {

    public enum Level {
        BASICO(0.4),
        MEDIO(0.7),
        NATIVO(1.0);

        private final double fluency;

        Level(double fluency) {
            this.fluency = fluency;
        }

        public double fluency() {
            return fluency;
        }
    }

    public LanguageSkill {
        if (code == null || !code.matches("[a-z]{2}")) {
            throw new IllegalArgumentException("el idioma se escribe con dos letras, como \"es\"");
        }
        if (level == null) {
            throw new IllegalArgumentException("falta el nivel del idioma " + code);
        }
    }

    public double fluency() {
        return level.fluency();
    }
}
