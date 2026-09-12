package com.sergisalas.olimpus.auth.application;

import com.sergisalas.olimpus.auth.domain.CodeSender;
import com.sergisalas.olimpus.auth.domain.EmailAddress;
import com.sergisalas.olimpus.auth.domain.LoginCode;
import com.sergisalas.olimpus.auth.domain.LoginCodeRepository;
import com.sergisalas.olimpus.auth.domain.Secrets;
import com.sergisalas.olimpus.shared.domain.Hasher;
import java.time.Clock;

/**
 * Caso de uso: alguien escribe su email y le llega un codigo.
 *
 * <p>No crea cuenta ni dice si el email ya estaba registrado: responde igual
 * en los dos casos, para que nadie pueda usar la app como lista de quien esta
 * dentro.
 */
public class RequestLoginCode {

    private final LoginCodeRepository codes;
    private final Secrets secrets;
    private final Hasher hasher;
    private final CodeSender sender;
    private final Clock clock;

    public RequestLoginCode(
            LoginCodeRepository codes,
            Secrets secrets,
            Hasher hasher,
            CodeSender sender,
            Clock clock) {
        this.codes = codes;
        this.secrets = secrets;
        this.hasher = hasher;
        this.sender = sender;
        this.clock = clock;
    }

    public void execute(String rawEmail) {
        EmailAddress email = EmailAddress.of(rawEmail);
        String code = secrets.sixDigitCode();

        codes.save(LoginCode.issued(email, hasher.hash(code), clock.instant()));
        sender.send(email, code);
    }
}
