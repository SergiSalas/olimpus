package com.sergisalas.olimpus.dev.adapter.in;

import com.sergisalas.olimpus.auth.adapter.in.CurrentAccount;
import com.sergisalas.olimpus.auth.domain.Account;
import com.sergisalas.olimpus.auth.domain.AccountRepository;
import com.sergisalas.olimpus.auth.domain.EmailAddress;
import com.sergisalas.olimpus.chat.application.CloseFinishedConversations;
import com.sergisalas.olimpus.chat.domain.Icebreakers;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.Match;
import com.sergisalas.olimpus.matching.domain.Origin;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import com.sergisalas.olimpus.profile.application.UploadPhoto;
import com.sergisalas.olimpus.profile.domain.Gender;
import com.sergisalas.olimpus.profile.domain.Location;
import com.sergisalas.olimpus.profile.domain.Profile;
import com.sergisalas.olimpus.profile.domain.ProfileNotFoundException;
import com.sergisalas.olimpus.profile.domain.ProfileRepository;
import com.sergisalas.olimpus.profile.domain.PromptAnswer;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.ImageIO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * A test partner and a remote control for time, so the whole flow can be walked
 * through from the phone without waiting hours or touching the database.
 *
 * <p>"Start" invents someone who fits whoever calls, hands them today's
 * conversation and lets {@link DemoBot} play the other side. "Advance" moves the
 * clock of that conversation: the rules themselves are untouched, only the
 * timestamps they read.
 *
 * <p>Only exists when {@code olimpus.dev-endpoints} is on, like the manual round.
 */
@RestController
@RequestMapping("/api/dev/demo")
@ConditionalOnProperty(name = "olimpus.dev-endpoints", havingValue = "true")
public class DemoController {

    /** Every test partner lives under this domain: that is how the bot finds them. */
    static final String DOMAIN = "demo.olimpus";

    private static final List<String> NICKNAMES =
            List.of("Marta", "Lucía", "Noa", "Alex", "Leo", "Dani", "Irene", "Pau");

    public record Started(UUID conversationId, String nickname) {}

    public record Advanced(String step, Instant opensAt, Instant closesAt) {}

    public enum Step {
        /** One hour less to wait: level 2 needs an hour since the first message. */
        HOUR,
        /** Four hours: the "I want to see you" button appears. */
        PHOTO,
        /** The last half hour: the final question opens. */
        DECISION,
        /** 22:00 now: the day is settled on the spot. */
        CLOSE
    }

    private final AccountRepository accounts;
    private final ProfileRepository profiles;
    private final ConversationRepository conversations;
    private final UploadPhoto uploadPhoto;
    private final CloseFinishedConversations closeFinished;
    private final RoundSchedule schedule;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public DemoController(
            AccountRepository accounts,
            ProfileRepository profiles,
            ConversationRepository conversations,
            UploadPhoto uploadPhoto,
            CloseFinishedConversations closeFinished,
            RoundSchedule schedule,
            JdbcTemplate jdbc,
            Clock clock) {
        this.accounts = accounts;
        this.profiles = profiles;
        this.conversations = conversations;
        this.uploadPhoto = uploadPhoto;
        this.closeFinished = closeFinished;
        this.schedule = schedule;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @PostMapping
    public Started start(@CurrentAccount Account account) {
        Instant now = clock.instant();
        LocalDate today = schedule.dateOf(now);
        Profile you =
                profiles.findByAccountId(account.id()).orElseThrow(ProfileNotFoundException::new);

        // One conversation a day: whatever you had open today makes room.
        conversations
                .openFor(account.id(), today)
                .ifPresent(old -> conversations.save(old.cancelled()));

        Profile partner = partnerFor(you, today, now);
        uploadPhoto.execute(partner.accountId(), photo(), "image/png");

        // Past 22:00 the real closing time is already gone: three more hours then.
        Instant closes = schedule.closesAt(today);
        if (closes.isBefore(now.plus(Duration.ofHours(1)))) closes = now.plus(Duration.ofHours(3));

        Conversation conversation =
                Conversation.opened(
                        new Match(account.id(), partner.accountId(), 0.9, Origin.BEST_MATCH),
                        today,
                        RoundKind.MAIN,
                        now.minusSeconds(60),
                        closes,
                        Icebreakers.rarestShared(you, partner).orElse(null));
        conversations.save(conversation);

        return new Started(conversation.id(), partner.nickname());
    }

    @PostMapping("/advance")
    public Advanced advance(@CurrentAccount Account account, @RequestParam Step step) {
        Instant now = clock.instant();
        Conversation conversation =
                conversations
                        .openFor(account.id(), schedule.dateOf(now))
                        .filter(c -> isDemo(c.partnerOf(account.id())))
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "No test conversation open today. Start one first."));

        switch (step) {
            case HOUR -> travel(conversation, Duration.ofHours(1));
            case PHOTO -> travel(conversation, Duration.ofHours(4));
            case DECISION -> closeAt(conversation, now.plus(Duration.ofMinutes(25)));
            case CLOSE -> {
                closeAt(conversation, now.minusSeconds(1));
                closeFinished.execute();
            }
        }

        Conversation after = conversations.byId(conversation.id()).orElseThrow();
        return new Advanced(step.name(), after.opensAt(), after.closesAt());
    }

    boolean isDemo(UUID accountId) {
        return accounts.findById(accountId)
                .map(a -> a.email().value().endsWith("@" + DOMAIN))
                .orElse(false);
    }

    /** Moves everything that already happened back in time, closing time excluded. */
    private void travel(Conversation conversation, Duration back) {
        long seconds = back.toSeconds();
        jdbc.update(
                "update message set sent_at = sent_at - make_interval(secs => ?) where conversation_id = ?",
                seconds,
                conversation.id());
        jdbc.update(
                "update conversation set opens_at = opens_at - make_interval(secs => ?),"
                        + " photo_wanted_by_a = photo_wanted_by_a - make_interval(secs => ?),"
                        + " photo_wanted_by_b = photo_wanted_by_b - make_interval(secs => ?)"
                        + " where id = ?",
                seconds,
                seconds,
                seconds,
                conversation.id());
    }

    private void closeAt(Conversation conversation, Instant closes) {
        // The conversation cannot close before it opened, so the opening moves too.
        Instant opens = conversation.opensAt().isBefore(closes) ? conversation.opensAt() : closes.minusSeconds(60);
        jdbc.update(
                "update conversation set opens_at = ?, closes_at = ? where id = ?",
                Timestamp.from(opens),
                Timestamp.from(closes),
                conversation.id());
    }

    /**
     * Someone who passes every filter with whoever asks: the gender they seek,
     * seeking theirs, an age inside their range, next door, same languages and
     * intent, and most of their interests.
     */
    private Profile partnerFor(Profile you, LocalDate today, Instant now) {
        var random = ThreadLocalRandom.current();
        String nickname = NICKNAMES.get(random.nextInt(NICKNAMES.size()));

        Account account =
                accounts.save(
                        Account.created(
                                EmailAddress.of(
                                        "%s-%s@%s"
                                                .formatted(
                                                        nickname.toLowerCase().replace("í", "i"),
                                                        UUID.randomUUID().toString().substring(0, 8),
                                                        DOMAIN)),
                                now));

        int yourAge = you.ageOn(today);
        int age = Math.max(you.ageMin(), Math.min(you.ageMax(), yourAge + 1));
        Gender gender = you.seeking().iterator().next();

        Set<String> interests = new LinkedHashSet<>(you.interests().stream().limit(5).toList());

        Profile partner =
                new Profile(
                        account.id(),
                        nickname,
                        today.minusYears(age).minusDays(30),
                        gender,
                        "",
                        Set.of(you.gender()),
                        Math.max(Profile.MIN_AGE, yourAge - 10),
                        Math.min(Profile.MAX_AGE, yourAge + 10),
                        100,
                        Location.rounded(
                                you.location().latitude() + 0.02, you.location().longitude() + 0.02),
                        you.languages(),
                        4,
                        4,
                        you.intent(),
                        interests,
                        List.of(
                                new PromptAnswer("last-hooked", "Un documental sobre pulpos. Muy fan."),
                                new PromptAnswer("always-ask", "Si tienes mascota, quiero fotos."),
                                new PromptAnswer("weird-habit", "Pongo nombre a todas mis plantas.")),
                        "Probadora profesional de apps",
                        "El servidor de desarrollo");
        profiles.save(partner);
        return partner;
    }

    /** A 600x800 picture: a pink-to-purple sky with a yellow sun. No people. */
    private static byte[] photo() {
        BufferedImage image = new BufferedImage(600, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(0xFF9EC0), 0, 800, new Color(0x8B5CF6)));
        g.fillRect(0, 0, 600, 800);
        g.setColor(new Color(0xFFC53D));
        g.fillOval(170, 200, 260, 260);
        g.dispose();
        try (var out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
