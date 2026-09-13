package com.sergisalas.olimpus.auth.application;

import com.sergisalas.olimpus.auth.domain.CodeSender;
import com.sergisalas.olimpus.auth.domain.EmailAddress;
import com.sergisalas.olimpus.auth.domain.LoginCode;
import com.sergisalas.olimpus.auth.domain.LoginCodeRepository;
import com.sergisalas.olimpus.auth.domain.Secrets;
import com.sergisalas.olimpus.shared.domain.Hasher;
import java.time.Clock;

/**
 * Use case: someone types their email and receives a code.
 *
 * <p>It does not create an account nor say whether the email was already
 * registered: it answers the same in both cases, so nobody can use the app as a
 * list of who is in.
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
