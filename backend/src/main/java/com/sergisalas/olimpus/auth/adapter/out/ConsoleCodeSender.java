package com.sergisalas.olimpus.auth.adapter.out;

import com.sergisalas.olimpus.auth.domain.CodeSender;
import com.sergisalas.olimpus.auth.domain.EmailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Development version: the code is written to the backend console and no
 * email is sent.
 *
 * <p>Before the beta it must be replaced by real delivery (Resend, Brevo, SES
 * or similar). That means changing this class and nothing else.
 */
@Component
public class ConsoleCodeSender implements CodeSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleCodeSender.class);

    @Override
    public void send(EmailAddress email, String code) {
        log.info("""

                ┌─────────────────────────────────────────────┐
                │  LOGIN CODE (development only)              │
                │  email: {}
                │  code:  {}
                └─────────────────────────────────────────────┘
                """, email.value(), code);
    }
}
