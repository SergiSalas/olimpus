package com.sergisalas.olimpus.shared.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * The language is the request's, or Spanish: never the machine's. A server set
 * up in English once answered in English to anyone who did not say otherwise.
 */
class MessagesTest {

    private final Locale machine = Locale.getDefault();

    @AfterEach
    void restore() {
        Locale.setDefault(machine);
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void outside_a_request_it_is_spanish_even_on_an_english_machine() {
        Locale.setDefault(Locale.ENGLISH);

        assertThat(messages().get("interest.chess")).isEqualTo("Ajedrez");
    }

    @Test
    void a_request_in_english_gets_english() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("en-GB"));

        assertThat(messages().get("interest.chess")).isEqualTo("Chess");
    }

    private static Messages messages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return new Messages(source);
    }
}
