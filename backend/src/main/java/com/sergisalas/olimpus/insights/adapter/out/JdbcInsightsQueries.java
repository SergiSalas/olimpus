package com.sergisalas.olimpus.insights.adapter.out;

import com.sergisalas.olimpus.insights.domain.Insights;
import com.sergisalas.olimpus.insights.domain.InsightsQueries;
import com.sergisalas.olimpus.matching.domain.Origin;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The counting, in SQL.
 *
 * <p>Nothing here is stored just to be measured: every number comes out of what
 * the app already writes down while it works. That was the point of keeping the
 * origin and the score with each conversation from day one.
 */
@Component
public class JdbcInsightsQueries implements InsightsQueries {

    private final JdbcTemplate jdbc;

    public JdbcInsightsQueries(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Insights.ByOrigin> outcomesBetween(LocalDate from, LocalDate to) {
        return jdbc.query(
                """
                select c.origin,
                       count(*) as pairs,
                       count(*) filter (
                           where c.messages_from_a > 0 and c.messages_from_b > 0) as both_wrote,
                       count(*) filter (where c.state = 'CONNECTED') as connections,
                       count(*) filter (
                           where c.state = 'CONNECTED'
                             and exists (
                                 select 1 from message m
                                 where m.conversation_id = c.id
                                   and m.sent_at >= c.closes_at + interval '48 hours')
                       ) as still_talking
                from conversation c
                where c.round_date between ? and ?
                group by c.origin
                order by c.origin
                """,
                (rs, row) ->
                        new Insights.ByOrigin(
                                Origin.valueOf(rs.getString("origin")),
                                rs.getInt("pairs"),
                                rs.getInt("both_wrote"),
                                rs.getInt("connections"),
                                rs.getInt("still_talking")),
                Date.valueOf(from),
                Date.valueOf(to));
    }

    @Override
    public int reportsBetween(LocalDate from, LocalDate to) {
        return count(
                """
                select count(*) from block
                where reason is not null and created_at::date between ? and ?
                """,
                from,
                to);
    }

    @Override
    public int blocksBetween(LocalDate from, LocalDate to) {
        return count("select count(*) from block where created_at::date between ? and ?", from, to);
    }

    @Override
    public int peopleWithNoMatchInAWeek(LocalDate today) {
        Integer value =
                jdbc.queryForObject(
                        """
                        select count(*) from profile p
                        where not exists (
                            select 1 from conversation c
                            where (c.account_a = p.account_id or c.account_b = p.account_id)
                              and c.round_date > ?
                              and c.state <> 'CANCELLED')
                        """,
                        Integer.class,
                        Date.valueOf(today.minusDays(7)));
        return value == null ? 0 : value;
    }

    @Override
    public double averageDaysWaiting(LocalDate today) {
        Double value =
                jdbc.queryForObject(
                        """
                        -- greatest(0, ...): a round handed out for tomorrow would
                        -- otherwise make somebody "wait" a negative number of days.
                        select coalesce(
                                   avg(greatest(0, ?::date - greatest(last_day, p.created_at::date))),
                                   0)
                        from profile p
                        left join lateral (
                            select max(c.round_date) as last_day
                            from conversation c
                            where (c.account_a = p.account_id or c.account_b = p.account_id)
                              and c.state <> 'CANCELLED'
                        ) as last_one on true
                        """,
                        Double.class,
                        Date.valueOf(today));
        return value == null ? 0 : value;
    }

    @Override
    public int diedInSilenceBetween(LocalDate from, LocalDate to) {
        return count(
                """
                select count(*) from conversation
                where round_date between ? and ?
                  and messages_from_a = 0 and messages_from_b = 0
                """,
                from,
                to);
    }

    @Override
    public List<Integer> conversationsPerPerson(LocalDate from, LocalDate to) {
        return jdbc.queryForList(
                """
                select count(c.id) as conversations
                from profile p
                left join conversation c
                       on (c.account_a = p.account_id or c.account_b = p.account_id)
                      and c.round_date between ? and ?
                      and c.state <> 'CANCELLED'
                group by p.account_id
                """,
                Integer.class,
                Date.valueOf(from),
                Date.valueOf(to));
    }

    private int count(String sql, LocalDate from, LocalDate to) {
        Integer value =
                jdbc.queryForObject(sql, Integer.class, Date.valueOf(from), Date.valueOf(to));
        return value == null ? 0 : value;
    }
}
