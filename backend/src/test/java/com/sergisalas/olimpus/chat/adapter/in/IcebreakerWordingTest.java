package com.sergisalas.olimpus.chat.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.profile.domain.InterestCatalog;
import com.sergisalas.olimpus.shared.adapter.Messages;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Reads the real messages.properties, without starting Spring: the texts are
 * part of the product, and a missing one should fail here, not on a phone.
 */
class IcebreakerWordingTest {

    private final Messages messages = new Messages(realMessageSource());
    private final IcebreakerWording wording = new IcebreakerWording(messages);

    @Test
    void the_question_names_the_shared_interest_and_asks_about_it() {
        assertThat(wording.wordingFor("climbing"))
                .isEqualTo("Los dos habéis puesto escalada: ¿montaña o rocódromo?");
    }

    @Test
    void the_interest_is_shown_by_its_label_not_its_id() {
        assertThat(wording.wordingFor("board-games"))
                .contains("juegos de mesa")
                .doesNotContain("board-games");
    }

    @Test
    void with_nothing_in_common_something_is_asked_anyway() {
        assertThat(wording.wordingFor(null)).contains("¿qué es lo último que te ha enganchado?");
    }

    @Test
    void every_catalog_interest_has_its_label_and_its_own_question() {
        String fallback = messages.get("icebreaker.question.fallback");

        for (var entry : InterestCatalog.ENTRIES) {
            assertThat(messages.interestLabel(entry.name()))
                    .as("label for %s", entry.name())
                    .isNotEqualTo(entry.name());
            assertThat(wording.wordingFor(entry.name()))
                    .as("question for %s", entry.name())
                    .doesNotContain(fallback);
        }
    }

    private static ResourceBundleMessageSource realMessageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
