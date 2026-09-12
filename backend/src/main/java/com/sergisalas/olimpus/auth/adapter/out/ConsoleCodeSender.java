package com.sergisalas.olimpus.auth.adapter.out;

import com.sergisalas.olimpus.auth.domain.CodeSender;
import com.sergisalas.olimpus.auth.domain.EmailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Version de desarrollo: el codigo se escribe en la consola del backend y no
 * se envia ningun correo.
 *
 * <p>Antes de la beta hay que sustituirla por un envio real (Resend, Brevo,
 * SES o similar). Es cambiar esta clase y nada mas.
 */
@Component
public class ConsoleCodeSender implements CodeSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleCodeSender.class);

    @Override
    public void send(EmailAddress email, String code) {
        log.info("""

                ┌─────────────────────────────────────────────┐
                │  CODIGO DE ACCESO (solo en desarrollo)      │
                │  email:  {}
                │  codigo: {}
                └─────────────────────────────────────────────┘
                """, email.value(), code);
    }
}
