package com.sergisalas.olimpus.auth.adapter.out;

import com.sergisalas.olimpus.auth.domain.CodeSender;
import com.sergisalas.olimpus.auth.domain.EmailAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Development version: the code is written to the backend console and no
 * email is sent.
 *
 * <p>Before the beta it must be replaced by real delivery (Resend, Brevo, SES
 * or similar). That means changing this class and nothing else.
 *
 * <p>It also remembers the last code of each email, only in memory, so the dev
 * endpoint can hand it to the app while testing (see DevCodeController).
 */
@Component
public class ConsoleCodeSender implements CodeSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleCodeSender.class);

    private final Map<String, String> lastCodes = new ConcurrentHashMap<>();

    @Override
    public void send(EmailAddress email, String code) {
        lastCodes.put(email.value(), code);
        log.info("""

                ┌─────────────────────────────────────────────┐
                │  LOGIN CODE (development only)              │
                │  email: {}
                │  code:  {}
                └─────────────────────────────────────────────┘
                """, email.value(), code);
    }

    /** The last code sent to that email, if any. */
    public Optional<String> lastCodeFor(EmailAddress email) {
        return Optional.ofNullable(lastCodes.get(email.value()));
    }
}
