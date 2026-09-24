package com.sergisalas.olimpus.profile.domain;

import com.sergisalas.olimpus.shared.domain.RuleViolationException;

/**
 * One of the catalogue's questions and what this person answered.
 *
 * <p>The question is stored by id, not by its text: the wording can be fixed or
 * translated later without touching what anyone wrote.
 */
public record PromptAnswer(String question, String answer) {

    public static final int MAX_ANSWER = 200;

    public PromptAnswer {
        if (question == null || !PromptCatalog.contains(question)) {
            throw new RuleViolationException("prompt.question.unknown", String.valueOf(question));
        }

        answer = answer == null ? "" : answer.trim();
        if (answer.isEmpty()) {
            throw new RuleViolationException("prompt.answer.empty");
        }
        if (answer.length() > MAX_ANSWER) {
            throw new RuleViolationException("prompt.answer.too-long", MAX_ANSWER);
        }
    }
}
