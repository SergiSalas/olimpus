package com.sergisalas.olimpus.matching.adapter.out;

import com.sergisalas.olimpus.matching.domain.InterestWeights;
import com.sergisalas.olimpus.matching.domain.MatchContext;
import com.sergisalas.olimpus.matching.domain.MatchContextFactory;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Reune de la base de datos la historia que el reparto necesita: bloqueos, con
 * quien hablo cada uno y cuando, cuantas conversaciones lleva y cuantos dias
 * lleva esperando.
 *
 * <p>Son cuatro consultas sobre toda la poblacion, no una por persona: a escala
 * de una ciudad se hace de una vez y el reparto trabaja despues en memoria.
 */
@Component
public class JdbcMatchContextFactory implements MatchContextFactory {

    private final JdbcTemplate jdbc;

    public JdbcMatchContextFactory(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public MatchContext forRound(LocalDate today, List<Profile> pool) {
        MatchContext.Builder builder =
                MatchContext.on(today).interestWeights(InterestWeights.fromPopulation(pool));

        jdbc.query(
                "select blocker, blocked from block",
                rs -> {
                    builder.blocked(
                            rs.getObject("blocker", UUID.class), rs.getObject("blocked", UUID.class));
                });

        // Ultimo dia en que cada pareja hablo: alimenta el factor de novedad, que
        // permite volver a coincidir pasado un tiempo en vez de bloquear para
        // siempre.
        jdbc.query(
                """
                select account_a, account_b, max(round_date) as ultima
                from conversation
                where state <> 'CANCELADA'
                group by account_a, account_b
                """,
                rs -> {
                    builder.talked(
                            rs.getObject("account_a", UUID.class),
                            rs.getObject("account_b", UUID.class),
                            rs.getDate("ultima").toLocalDate());
                });

        // Conversaciones por persona (las dos columnas, en una sola pasada).
        jdbc.query(
                """
                select cuenta, count(*) as cuantas, max(round_date) as ultima
                from (
                    select account_a as cuenta, round_date from conversation where state <> 'CANCELADA'
                    union all
                    select account_b as cuenta, round_date from conversation where state <> 'CANCELADA'
                ) as todas
                group by cuenta
                """,
                rs -> {
                    UUID cuenta = rs.getObject("cuenta", UUID.class);
                    builder.conversations(cuenta, rs.getInt("cuantas"));
                    LocalDate ultima = rs.getDate("ultima").toLocalDate();
                    builder.waiting(
                            cuenta,
                            (int) java.time.temporal.ChronoUnit.DAYS.between(ultima, today));
                });

        // Quien nunca ha tenido conversacion lleva esperando desde que se
        // registro: si no, no se le relajaria nada y podria no salir nunca.
        jdbc.query(
                """
                select p.account_id, p.created_at::date as desde
                from profile p
                where not exists (
                    select 1 from conversation c
                    where c.state <> 'CANCELADA'
                      and (c.account_a = p.account_id or c.account_b = p.account_id))
                """,
                rs -> {
                    builder.waiting(
                            rs.getObject("account_id", UUID.class),
                            (int)
                                    java.time.temporal.ChronoUnit.DAYS.between(
                                            rs.getDate("desde").toLocalDate(), today));
                });

        return builder.build();
    }
}
