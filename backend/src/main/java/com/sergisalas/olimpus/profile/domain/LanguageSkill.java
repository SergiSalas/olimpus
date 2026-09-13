package com.sergisalas.olimpus.profile.domain;

import com.sergisalas.olimpus.shared.domain.RuleViolationException;

/**
 * A language and how well it is spoken. The level is asked in words and the
 * algorithm uses it as a number between 0 and 1.
 */
public record LanguageSkill(String code, Level level) {

    public enum Level {
        BASIC(0.4),
        INTERMEDIATE(0.7),
        NATIVE(1.0);

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
            throw new RuleViolationException("language.code.invalid");
        }
        if (level == null) {
            throw new RuleViolationException("language.level.missing", code);
        }
    }

    public double fluency() {
        return level.fluency();
    }
}
