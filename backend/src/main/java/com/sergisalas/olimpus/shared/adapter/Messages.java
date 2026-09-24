package com.sergisalas.olimpus.shared.adapter;

import com.sergisalas.olimpus.shared.domain.UserFacingError;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * The texts a person reads, in their language. They live in
 * {@code resources/messages.properties}, never in the code.
 *
 * <p>Only adapters use this: the domain speaks in message keys (see
 * {@link UserFacingError}) and knows nothing about languages.
 */
@Component
public class Messages {

    /** Spanish, the launch community's language. */
    static final Locale DEFAULT = Locale.forLanguageTag("es");

    private final MessageSource source;

    public Messages(MessageSource source) {
        this.source = source;
    }

    public String get(String key, Object... args) {
        return source.getMessage(key, args, locale());
    }

    public String getOrDefault(String key, String defaultText) {
        return source.getMessage(key, null, defaultText, locale());
    }

    public String of(UserFacingError error) {
        return get(error.messageKey(), error.messageArgs());
    }

    /** "ice-climbing" -> "Escalada en hielo". Falls back to the id if it has no label. */
    public String interestLabel(String interest) {
        return getOrDefault("interest." + interest, interest);
    }

    /** "always-ask" -> "Una pregunta que siempre acabo haciendo". */
    public String promptLabel(String question) {
        return getOrDefault("prompt." + question, question);
    }

    /** For texts nobody asked for (notifications): the language is given, not read. */
    public String getIn(Locale locale, String key, Object... args) {
        return source.getMessage(key, args, locale);
    }

    /**
     * The request's language. Outside a request (a scheduled task, a test) there
     * is none, and then it is Spanish: never the language of whatever machine the
     * server happens to run on.
     */
    public Locale locale() {
        LocaleContext context = LocaleContextHolder.getLocaleContext();
        Locale locale = context == null ? null : context.getLocale();
        return locale == null ? DEFAULT : locale;
    }
}
