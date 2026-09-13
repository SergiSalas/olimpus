package com.sergisalas.olimpus.matching.adapter.out;

import com.sergisalas.olimpus.matching.domain.InterestWeights;
import com.sergisalas.olimpus.matching.domain.MatchContext;
import com.sergisalas.olimpus.matching.domain.MatchContextFactory;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Gathers from the database the history matching needs: blocks, who talked to
 * whom and when, how many conversations each person has had and how many days
 * they have been waiting.
 *
 * <p>Four queries over the whole population, not one per person: at city scale
 * it is done in one go and matching then works in memory.
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

        // Last day each pair talked: feeds the novelty factor, which lets people
        // meet again after a while instead of blocking them forever.
        jdbc.query(
                """
                select account_a, account_b, max(round_date) as last_day
                from conversation
                where state <> 'CANCELLED'
                group by account_a, account_b
                """,
                rs -> {
                    builder.talked(
                            rs.getObject("account_a", UUID.class),
                            rs.getObject("account_b", UUID.class),
                            rs.getDate("last_day").toLocalDate());
                });

        // Conversations per person (both columns, in a single pass).
        jdbc.query(
                """
                select account, count(*) as how_many, max(round_date) as last_day
                from (
                    select account_a as account, round_date from conversation where state <> 'CANCELLED'
                    union all
                    select account_b as account, round_date from conversation where state <> 'CANCELLED'
                ) as everyone
                group by account
                """,
                rs -> {
                    UUID account = rs.getObject("account", UUID.class);
                    builder.conversations(account, rs.getInt("how_many"));
                    LocalDate lastDay = rs.getDate("last_day").toLocalDate();
                    builder.waiting(account, (int) ChronoUnit.DAYS.between(lastDay, today));
                });

        // Whoever never had a conversation has been waiting since they signed up:
        // otherwise nothing would be relaxed for them and they might never appear.
        jdbc.query(
                """
                select p.account_id, p.created_at::date as since
                from profile p
                where not exists (
                    select 1 from conversation c
                    where c.state <> 'CANCELLED'
                      and (c.account_a = p.account_id or c.account_b = p.account_id))
                """,
                rs -> {
                    builder.waiting(
                            rs.getObject("account_id", UUID.class),
                            (int) ChronoUnit.DAYS.between(rs.getDate("since").toLocalDate(), today));
                });

        return builder.build();
    }
}
