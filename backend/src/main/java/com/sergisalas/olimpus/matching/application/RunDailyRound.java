package com.sergisalas.olimpus.matching.application;

import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.DailyRound;
import com.sergisalas.olimpus.matching.domain.Match;
import com.sergisalas.olimpus.matching.domain.MatchContext;
import com.sergisalas.olimpus.matching.domain.MatchContextFactory;
import com.sergisalas.olimpus.matching.domain.ProfileDirectory;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caso de uso: la ronda del dia.
 *
 * <p>A las 4:00 reparte a todo el mundo. A las 14:00 hace la repesca: primero
 * cancela las conversaciones que siguen en silencio (nadie ha escrito nada) y
 * despues vuelve a repartir a quien se haya quedado sin nada.
 *
 * <p>Es repetible a proposito: la semilla del azar sale del dia y del tipo de
 * ronda, asi que la misma ronda se puede volver a calcular igual para revisarla.
 */
public class RunDailyRound {

    /** Lo que ha hecho una ronda. Sirve para el log y para los avisos. */
    public record RoundResult(
            LocalDate date,
            RoundKind kind,
            int peopleInPool,
            List<Conversation> created,
            List<UUID> leftOut,
            List<Conversation> cancelled,
            boolean alreadyRan) {

        public static RoundResult skipped(LocalDate date, RoundKind kind) {
            return new RoundResult(date, kind, 0, List.of(), List.of(), List.of(), true);
        }
    }

    private final ProfileDirectory profiles;
    private final ConversationRepository conversations;
    private final MatchContextFactory contexts;
    private final RoundSchedule schedule;

    public RunDailyRound(
            ProfileDirectory profiles,
            ConversationRepository conversations,
            MatchContextFactory contexts,
            RoundSchedule schedule) {
        this.profiles = profiles;
        this.conversations = conversations;
        this.contexts = contexts;
        this.schedule = schedule;
    }

    public RoundResult execute(LocalDate date, RoundKind kind) {
        List<Conversation> deHoy = conversations.byDate(date);

        // Que la ronda se lance dos veces (un reinicio, una tarea repetida) no
        // puede repartir dos veces.
        boolean yaCorrio = deHoy.stream().anyMatch(c -> c.roundKind() == kind);
        if (yaCorrio) {
            return RoundResult.skipped(date, kind);
        }

        List<Conversation> canceladas = new ArrayList<>();
        if (kind == RoundKind.REPESCA) {
            for (Conversation conversation : deHoy) {
                if (conversation.isOpen() && conversation.isSilent()) {
                    Conversation cancelada = conversation.cancelled();
                    conversations.save(cancelada);
                    canceladas.add(cancelada);
                }
            }
        }

        // Quien ya tiene conversacion viva hoy no entra: una nueva al dia.
        Set<UUID> ocupados =
                conversations.byDate(date).stream()
                        .filter(Conversation::isOpen)
                        .flatMap(c -> List.of(c.accountA(), c.accountB()).stream())
                        .collect(Collectors.toSet());

        List<Profile> pool =
                profiles.everyoneWithProfile().stream()
                        .filter(p -> !ocupados.contains(p.accountId()))
                        .toList();

        MatchContext ctx = contexts.forRound(date, pool);
        List<Match> matches = DailyRound.plan(pool, ctx, semillaDe(date, kind));

        Instant abre = schedule.opensAt(date, kind);
        Instant cierra = schedule.closesAt(date);

        List<Conversation> creadas = new ArrayList<>();
        for (Match match : matches) {
            Conversation conversation = Conversation.opened(match, date, kind, abre, cierra);
            conversations.save(conversation);
            creadas.add(conversation);
        }

        return new RoundResult(
                date, kind, pool.size(), creadas, DailyRound.leftOut(pool, matches), canceladas, false);
    }

    /** Misma fecha y mismo tipo de ronda, mismo reparto. */
    private static Random semillaDe(LocalDate date, RoundKind kind) {
        return new Random(date.toEpochDay() * 31 + kind.ordinal());
    }
}
