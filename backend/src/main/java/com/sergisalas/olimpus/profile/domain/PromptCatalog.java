package com.sergisalas.olimpus.profile.domain;

import java.util.List;

/**
 * The questions someone answers instead of writing a bio.
 *
 * <p>An empty box asking you to describe yourself gets filled with a shrug. A
 * concrete question gets an answer, and the answer gives the other person
 * something to reply to, which is the whole point: this is what opens at level
 * 2, when a conversation is already running and needs material.
 *
 * <p>The questions are chosen to be answerable in one line and to invite a
 * follow-up. Ids are language-neutral; the text each person reads lives in
 * {@code messages.properties} under {@code prompt.<id>}.
 */
public final class PromptCatalog {

    public static final List<String> QUESTIONS =
            List.of(
                    "last-hooked",
                    "always-ask",
                    "weird-habit",
                    "makes-me-laugh",
                    "perfect-tuesday",
                    "learned-late",
                    "hill-to-die-on",
                    "surprisingly-good-at",
                    "always-return",
                    "changed-my-mind",
                    "too-much-internet",
                    "want-to-try",
                    "worst-recommendation",
                    "gives-me-away",
                    "not-in-my-interests");

    private PromptCatalog() {}

    public static boolean contains(String question) {
        return QUESTIONS.contains(question);
    }
}
