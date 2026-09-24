package com.sergisalas.olimpus.profile.adapter.out;

import com.sergisalas.olimpus.profile.domain.Gender;
import com.sergisalas.olimpus.profile.domain.Intent;
import com.sergisalas.olimpus.profile.domain.LanguageSkill;
import com.sergisalas.olimpus.profile.domain.Location;
import com.sergisalas.olimpus.profile.domain.Profile;
import com.sergisalas.olimpus.profile.domain.ProfileRepository;
import com.sergisalas.olimpus.profile.domain.PromptAnswer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcProfileRepository implements ProfileRepository {

    private final JdbcTemplate jdbc;

    public JdbcProfileRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Profile> findByAccountId(UUID accountId) {
        List<LanguageSkill> languages =
                jdbc.query(
                        "select code, level from profile_language where account_id = ? order by code",
                        (rs, row) ->
                                new LanguageSkill(
                                        rs.getString("code"),
                                        LanguageSkill.Level.valueOf(rs.getString("level"))),
                        accountId);

        List<PromptAnswer> prompts =
                jdbc.query(
                        "select question, answer from profile_prompt where account_id = ? order by position",
                        (rs, row) ->
                                new PromptAnswer(rs.getString("question"), rs.getString("answer")),
                        accountId);

        return jdbc.query(
                        """
                        select account_id, nickname, birth_date, gender, gender_label, seeking,
                               age_min, age_max, max_distance_km, latitude, longitude,
                               sociability, conversation_depth, intent, interests,
                               occupation, from_place
                        from profile where account_id = ?
                        """,
                        (rs, row) ->
                                new Profile(
                                        rs.getObject("account_id", UUID.class),
                                        rs.getString("nickname"),
                                        rs.getDate("birth_date").toLocalDate(),
                                        Gender.valueOf(rs.getString("gender")),
                                        rs.getString("gender_label"),
                                        toGenders(rs.getArray("seeking")),
                                        rs.getInt("age_min"),
                                        rs.getInt("age_max"),
                                        rs.getInt("max_distance_km"),
                                        new Location(rs.getDouble("latitude"), rs.getDouble("longitude")),
                                        languages,
                                        rs.getInt("sociability"),
                                        rs.getInt("conversation_depth"),
                                        Intent.valueOf(rs.getString("intent")),
                                        toStrings(rs.getArray("interests")),
                                        prompts,
                                        rs.getString("occupation"),
                                        rs.getString("from_place")),
                        accountId)
                .stream()
                .findFirst();
    }

    /**
     * Profile, languages and prompts are saved together or not at all: one
     * single transaction.
     */
    @Override
    @Transactional
    public void save(Profile profile) {
        jdbc.update(
                """
                insert into profile (
                    account_id, nickname, birth_date, gender, gender_label, seeking,
                    age_min, age_max, max_distance_km, latitude, longitude,
                    sociability, conversation_depth, intent, interests,
                    occupation, from_place)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (account_id) do update set
                    nickname = excluded.nickname,
                    birth_date = excluded.birth_date,
                    gender = excluded.gender,
                    gender_label = excluded.gender_label,
                    seeking = excluded.seeking,
                    age_min = excluded.age_min,
                    age_max = excluded.age_max,
                    max_distance_km = excluded.max_distance_km,
                    latitude = excluded.latitude,
                    longitude = excluded.longitude,
                    sociability = excluded.sociability,
                    conversation_depth = excluded.conversation_depth,
                    intent = excluded.intent,
                    interests = excluded.interests,
                    occupation = excluded.occupation,
                    from_place = excluded.from_place,
                    updated_at = now()
                """,
                profile.accountId(),
                profile.nickname(),
                java.sql.Date.valueOf(profile.birthDate()),
                profile.gender().name(),
                profile.genderLabel(),
                profile.seeking().stream().map(Gender::name).toArray(String[]::new),
                profile.ageMin(),
                profile.ageMax(),
                profile.maxDistanceKm(),
                profile.location().latitude(),
                profile.location().longitude(),
                profile.sociability(),
                profile.conversationDepth(),
                profile.intent().name(),
                profile.interests().toArray(String[]::new),
                profile.occupation(),
                profile.fromPlace());

        jdbc.update("delete from profile_language where account_id = ?", profile.accountId());
        for (LanguageSkill language : profile.languages()) {
            jdbc.update(
                    "insert into profile_language (account_id, code, level) values (?, ?, ?)",
                    profile.accountId(),
                    language.code(),
                    language.level().name());
        }

        jdbc.update("delete from profile_prompt where account_id = ?", profile.accountId());
        List<PromptAnswer> prompts = profile.prompts();
        for (int position = 0; position < prompts.size(); position++) {
            jdbc.update(
                    "insert into profile_prompt (account_id, position, question, answer) values (?, ?, ?, ?)",
                    profile.accountId(),
                    position,
                    prompts.get(position).question(),
                    prompts.get(position).answer());
        }
    }

    private static Set<Gender> toGenders(java.sql.Array array) throws java.sql.SQLException {
        return toStrings(array).stream().map(Gender::valueOf).collect(Collectors.toSet());
    }

    private static Set<String> toStrings(java.sql.Array array) throws java.sql.SQLException {
        if (array == null) return Set.of();
        return new LinkedHashSet<>(new ArrayList<>(List.of((String[]) array.getArray())));
    }
}
