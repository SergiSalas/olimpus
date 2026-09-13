package com.sergisalas.olimpus.chat.adapter.in;

import com.sergisalas.olimpus.shared.adapter.Messages;
import org.springframework.stereotype.Component;

/**
 * Turns the interest a conversation opens with into the question both people
 * read, e.g. "You both picked kendo: how long have you been practising?" in the
 * reader's language.
 */
@Component
public class IcebreakerWording {

    private final Messages messages;

    public IcebreakerWording(Messages messages) {
        this.messages = messages;
    }

    /** @param interest the shared interest, or null when they share none */
    public String wordingFor(String interest) {
        if (interest == null) {
            return messages.get("icebreaker.nothing-in-common");
        }
        String question =
                messages.getOrDefault(
                        "icebreaker.question." + interest, messages.get("icebreaker.question.fallback"));
        String label = messages.interestLabel(interest).toLowerCase(messages.locale());
        return messages.get("icebreaker.shared", label, question);
    }
}
