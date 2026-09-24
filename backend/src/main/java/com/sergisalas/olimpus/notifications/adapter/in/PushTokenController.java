package com.sergisalas.olimpus.notifications.adapter.in;

import com.sergisalas.olimpus.auth.adapter.in.CurrentAccount;
import com.sergisalas.olimpus.auth.domain.Account;
import com.sergisalas.olimpus.notifications.domain.PushTokenRepository;
import com.sergisalas.olimpus.shared.domain.RuleViolationException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Where to reach this person's phone, and in which language. Sent by the app once
 * it has permission, and again when its language changes.
 */
@RestController
@RequestMapping("/api/push-token")
public class PushTokenController {

    public record TokenRequest(String token) {}

    private final PushTokenRepository tokens;

    public PushTokenController(PushTokenRepository tokens) {
        this.tokens = tokens;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@CurrentAccount Account account, @RequestBody TokenRequest body) {
        if (body == null || body.token() == null || body.token().isBlank()) {
            throw new RuleViolationException("push-token.missing");
        }
        // The app sends its language in Accept-Language on every request, this one
        // included: that is the language this phone's notifications will be in.
        tokens.save(
                account.id(), body.token().trim(), LocaleContextHolder.getLocale().getLanguage());
    }
}
