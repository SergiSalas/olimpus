package com.sergisalas.olimpus.safety.adapter.in;

import com.sergisalas.olimpus.auth.adapter.in.CurrentAccount;
import com.sergisalas.olimpus.auth.domain.Account;
import com.sergisalas.olimpus.safety.application.ReportAndBlock;
import com.sergisalas.olimpus.safety.domain.ReportReason;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations/{id}")
public class SafetyController {

    /** @param reason null blocks without reporting */
    public record ReportRequest(ReportReason reason) {}

    private final ReportAndBlock reportAndBlock;

    public SafetyController(ReportAndBlock reportAndBlock) {
        this.reportAndBlock = reportAndBlock;
    }

    /**
     * One tap: the conversation is cut right now and they never meet again. It
     * answers 204 and nothing else, because there is nothing to negotiate.
     */
    @PostMapping("/report")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(
            @PathVariable UUID id,
            @CurrentAccount Account account,
            @RequestBody(required = false) ReportRequest body) {

        reportAndBlock.execute(id, account.id(), body == null ? null : body.reason());
    }
}
