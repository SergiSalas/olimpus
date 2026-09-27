package com.sergisalas.olimpus.dev.adapter.in;

import com.sergisalas.olimpus.auth.adapter.out.ConsoleCodeSender;
import com.sergisalas.olimpus.auth.domain.EmailAddress;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The last login code sent to an email, so the app can fill it in while
 * testing instead of someone copying it from the console.
 *
 * <p>Only exists with {@code olimpus.dev-endpoints} on. It also needs the
 * console sender: once codes go out by real email, there is nothing to hand
 * over and it answers 404.
 */
@RestController
@RequestMapping("/api/dev/last-code")
@ConditionalOnProperty(name = "olimpus.dev-endpoints", havingValue = "true")
public class DevCodeController {

    public record LastCode(String code) {}

    private final ObjectProvider<ConsoleCodeSender> console;

    public DevCodeController(ObjectProvider<ConsoleCodeSender> console) {
        this.console = console;
    }

    @GetMapping
    public LastCode lastCode(@RequestParam String email) {
        ConsoleCodeSender sender = console.getIfAvailable();
        Optional<String> code =
                sender == null ? Optional.empty() : sender.lastCodeFor(EmailAddress.of(email));
        return code.map(LastCode::new)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "No code sent to that email yet."));
    }
}
