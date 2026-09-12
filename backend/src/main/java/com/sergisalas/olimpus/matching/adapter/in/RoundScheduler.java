package com.sergisalas.olimpus.matching.adapter.in;

import com.sergisalas.olimpus.matching.application.RunDailyRound;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Las dos tareas del dia. Es el unico sitio que sabe de relojes de sistema: el
 * caso de uso recibe la fecha, asi que en los tests no hay que esperar a las
 * 4 de la manana.
 */
@Component
public class RoundScheduler {

    private static final Logger log = LoggerFactory.getLogger(RoundScheduler.class);

    private final RunDailyRound runDailyRound;
    private final RoundSchedule schedule;
    private final Clock clock;

    public RoundScheduler(RunDailyRound runDailyRound, RoundSchedule schedule, Clock clock) {
        this.runDailyRound = runDailyRound;
        this.schedule = schedule;
        this.clock = clock;
    }

    @Scheduled(cron = "${olimpus.rondas.principal:0 0 4 * * *}", zone = "${olimpus.zona:Europe/Madrid}")
    public void rondaPrincipal() {
        lanzar(RoundKind.PRINCIPAL);
    }

    @Scheduled(cron = "${olimpus.rondas.repesca:0 0 14 * * *}", zone = "${olimpus.zona:Europe/Madrid}")
    public void repesca() {
        lanzar(RoundKind.REPESCA);
    }

    private void lanzar(RoundKind kind) {
        var resultado = runDailyRound.execute(schedule.dateOf(clock.instant()), kind);
        if (resultado.alreadyRan()) {
            log.info("Ronda {} del {}: ya se habia repartido, no se toca nada.", kind, resultado.date());
            return;
        }
        log.info(
                "Ronda {} del {}: {} personas, {} conversaciones, {} sin pareja, {} canceladas por silencio.",
                kind,
                resultado.date(),
                resultado.peopleInPool(),
                resultado.created().size(),
                resultado.leftOut().size(),
                resultado.cancelled().size());
    }
}
