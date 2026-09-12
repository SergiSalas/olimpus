package com.sergisalas.olimpus.matching.adapter.in;

import com.sergisalas.olimpus.matching.application.RunDailyRound;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lanzar una ronda a mano, para no tener que esperar a las 4:00 mientras se
 * programa.
 *
 * <p>Solo existe si {@code olimpus.dev-endpoints} esta activado, y en la beta
 * tiene que estar apagado.
 */
@RestController
@RequestMapping("/api/dev")
@ConditionalOnProperty(name = "olimpus.dev-endpoints", havingValue = "true")
public class DevRoundController {

    public record RoundResponse(
            LocalDate date,
            RoundKind kind,
            int peopleInPool,
            int conversations,
            List<UUID> leftOut,
            int cancelled,
            boolean alreadyRan) {}

    private final RunDailyRound runDailyRound;
    private final RoundSchedule schedule;
    private final Clock clock;

    public DevRoundController(
            RunDailyRound runDailyRound, RoundSchedule schedule, Clock clock) {
        this.runDailyRound = runDailyRound;
        this.schedule = schedule;
        this.clock = clock;
    }

    @PostMapping("/round")
    public RoundResponse run(
            @RequestParam(defaultValue = "PRINCIPAL") RoundKind kind,
            @RequestParam(required = false) LocalDate date) {

        LocalDate dia = date != null ? date : schedule.dateOf(clock.instant());
        var resultado = runDailyRound.execute(dia, kind);

        return new RoundResponse(
                resultado.date(),
                resultado.kind(),
                resultado.peopleInPool(),
                resultado.created().size(),
                resultado.leftOut(),
                resultado.cancelled().size(),
                resultado.alreadyRan());
    }
}
