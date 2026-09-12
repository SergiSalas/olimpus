package com.sergisalas.olimpus.matching.adapter.in;

import com.sergisalas.olimpus.auth.adapter.in.CurrentAccount;
import com.sergisalas.olimpus.auth.domain.Account;
import com.sergisalas.olimpus.matching.application.GetTodaysConversation;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lo que la app pregunta al abrirse: ¿con quien hablo hoy? */
@RestController
@RequestMapping("/api")
public class TodayController {

    /**
     * Del otro solo viaja lo que el nivel permite ver. Si algun dia hiciera falta
     * mas, se añade aqui a proposito, no por descuido.
     */
    public record PartnerResponse(int age, List<String> interests, int approxDistanceKm, int level) {}

    public record TodayResponse(
            boolean hasConversation,
            UUID conversationId,
            Instant closesAt,
            PartnerResponse partner,
            List<String> sharedInterests,
            Instant nextRoundAt,
            String message) {}

    private final GetTodaysConversation getTodaysConversation;
    private final RoundSchedule schedule;
    private final Clock clock;

    public TodayController(
            GetTodaysConversation getTodaysConversation, RoundSchedule schedule, Clock clock) {
        this.getTodaysConversation = getTodaysConversation;
        this.schedule = schedule;
        this.clock = clock;
    }

    @GetMapping("/today")
    public TodayResponse today(@CurrentAccount Account account) {
        var hoy = getTodaysConversation.execute(account.id());

        if (hoy.conversation().isEmpty()) {
            return new TodayResponse(
                    false,
                    null,
                    null,
                    null,
                    List.of(),
                    proximaRonda(),
                    "Hoy todavía no tienes conversación. El reparto sale a las 4:00 y hay repesca a las 14:00.");
        }

        var conversation = hoy.conversation().get();
        var partner = hoy.partner().orElseThrow();

        return new TodayResponse(
                true,
                conversation.id(),
                conversation.closesAt(),
                new PartnerResponse(
                        partner.age(),
                        partner.interestsShown(),
                        partner.approxDistanceKm(),
                        partner.level()),
                hoy.sharedInterests(),
                null,
                null);
    }

    /** La proxima hora a la que puede aparecer alguien, para no esperar a ciegas. */
    private Instant proximaRonda() {
        Instant ahora = clock.instant();
        var hoy = schedule.dateOf(ahora);

        Instant repesca = schedule.opensAt(hoy, RoundKind.REPESCA);
        if (ahora.isBefore(repesca)) return repesca;

        return schedule.opensAt(hoy.plusDays(1), RoundKind.PRINCIPAL);
    }
}
