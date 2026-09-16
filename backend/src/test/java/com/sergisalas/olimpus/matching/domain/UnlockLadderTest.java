package com.sergisalas.olimpus.matching.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.chat.domain.Message;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UnlockLadderTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID LEO = UUID.randomUUID();
    private static final Instant START = Instant.parse("2026-09-13T09:00:00Z");

    private Conversation charla =
            new Conversation(
                    UUID.randomUUID(),
                    LocalDate.of(2026, 9, 13),
                    RoundKind.MAIN,
                    ANA,
                    LEO,
                    Origin.BEST_MATCH,
                    0.8,
                    Instant.parse("2026-09-13T02:00:00Z"),
                    Instant.parse("2026-09-13T20:00:00Z"),
                    ConversationState.OPEN,
                    0,
                    0,
                    "climbing",
                    null,
                    null,
                    null,
                    null);

    /** Messages one minute apart, taking turns as the pattern says: "AABA". */
    private static List<Message> conversation(String pattern) {
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < pattern.length(); i++) {
            UUID sender = pattern.charAt(i) == 'A' ? ANA : LEO;
            messages.add(
                    new Message(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            sender,
                            "mensaje " + i,
                            START.plusSeconds(60L * i)));
        }
        return messages;
    }

    private static Instant after(Duration duration) {
        return START.plus(duration);
    }

    @Nested
    class Turns {

        @Test
        void a_hundred_messages_in_a_row_are_one_turn() {
            var turns = UnlockLadder.turnsOf(charla, conversation("AAAAAAAAAA"));

            assertThat(turns.forA()).isEqualTo(1);
            assertThat(turns.forB()).isZero();
        }

        @Test
        void each_change_of_speaker_is_a_turn_for_whoever_starts_talking() {
            var turns = UnlockLadder.turnsOf(charla, conversation("AABBAB"));

            assertThat(turns.forA()).isEqualTo(2);
            assertThat(turns.forB()).isEqualTo(2);
        }

        @Test
        void no_messages_means_no_turns() {
            var turns = UnlockLadder.turnsOf(charla, List.of());

            assertThat(turns.forA()).isZero();
            assertThat(turns.forB()).isZero();
        }
    }

    @Nested
    class Levels {

        @Test
        void an_untouched_conversation_stays_at_level_zero() {
            assertThat(UnlockLadder.levelOf(charla, List.of(), START))
                    .isEqualTo(UnlockLevel.MATCH);
        }

        @Test
        void one_person_writing_alone_does_not_open_anything() {
            assertThat(UnlockLadder.levelOf(charla, conversation("AAAAA"), after(Duration.ofHours(5))))
                    .isEqualTo(UnlockLevel.MATCH);
        }

        @Test
        void when_both_have_written_the_nickname_appears() {
            assertThat(UnlockLadder.levelOf(charla, conversation("AB"), START.plusSeconds(120)))
                    .isEqualTo(UnlockLevel.FIRST_MESSAGE);
        }

        @Test
        void three_turns_each_are_not_enough_without_an_hour_behind_them() {
            List<Message> deEse = conversation("ABABAB");

            assertThat(UnlockLadder.turnsOf(charla, deEse).forA()).isEqualTo(3);
            assertThat(UnlockLadder.levelOf(charla, deEse, after(Duration.ofMinutes(59))))
                    .isEqualTo(UnlockLevel.FIRST_MESSAGE);
        }

        @Test
        void an_hour_is_not_enough_without_three_turns_each() {
            assertThat(UnlockLadder.levelOf(charla, conversation("ABAB"), after(Duration.ofHours(3))))
                    .isEqualTo(UnlockLevel.FIRST_MESSAGE);
        }

        @Test
        void with_both_the_bio_opens() {
            assertThat(UnlockLadder.levelOf(charla, conversation("ABABAB"), after(Duration.ofHours(1))))
                    .isEqualTo(UnlockLevel.CONVERSATION);
        }

        @Test
        void the_photo_needs_both_to_accept_not_just_more_talking() {
            List<Message> largo = conversation("ABABABABAB");

            assertThat(UnlockLadder.levelOf(charla, largo, after(Duration.ofHours(10))))
                    .isEqualTo(UnlockLevel.CONVERSATION);

            Conversation soloUno = charla.withPhotoWantedBy(ANA, after(Duration.ofHours(5)));
            assertThat(UnlockLadder.levelOf(soloUno, largo, after(Duration.ofHours(10))))
                    .isEqualTo(UnlockLevel.CONVERSATION);

            Conversation losDos = soloUno.withPhotoWantedBy(LEO, after(Duration.ofHours(6)));
            assertThat(UnlockLadder.levelOf(losDos, largo, after(Duration.ofHours(10))))
                    .isEqualTo(UnlockLevel.GOOD_CONNECTION);
        }
    }

    @Nested
    class AskingForThePhoto {

        @Test
        void cannot_be_asked_for_before_the_conversation_level() {
            assertThat(UnlockLadder.canAskForPhoto(charla, conversation("AB"), after(Duration.ofHours(6))))
                    .isFalse();
        }

        @Test
        void cannot_be_asked_for_before_four_hours_even_with_plenty_of_turns() {
            List<Message> largo = conversation("ABABABABAB");

            assertThat(UnlockLadder.canAskForPhoto(charla, largo, after(Duration.ofHours(3))))
                    .isFalse();
            assertThat(UnlockLadder.canAskForPhoto(charla, largo, after(Duration.ofHours(4))))
                    .isTrue();
        }
    }

    @Nested
    class AskingTwice {

        @Test
        void does_not_move_the_moment_it_was_first_asked() {
            Instant primera = after(Duration.ofHours(4));
            Conversation unaVez = charla.withPhotoWantedBy(ANA, primera);
            Conversation dosVeces = unaVez.withPhotoWantedBy(ANA, after(Duration.ofHours(9)));

            assertThat(dosVeces.photoWantedByA()).isEqualTo(primera);
            assertThat(dosVeces.bothWantPhoto()).isFalse();
        }

        @Test
        void nobody_learns_that_the_other_one_asked() {
            Conversation pedida = charla.withPhotoWantedBy(ANA, after(Duration.ofHours(4)));

            assertThat(pedida.photoWantedBy(ANA)).isTrue();
            assertThat(pedida.photoWantedBy(LEO)).isFalse();
            assertThat(pedida.bothWantPhoto()).isFalse();
        }
    }

    @Nested
    class WhenEachLevelOpened {

        @Test
        void the_reply_that_gets_both_talking_is_the_one_that_opens_level_one() {
            List<Message> messages = conversation("AAB");

            var unlocks = UnlockLadder.unlocksOf(charla, messages, after(Duration.ofMinutes(5)));

            assertThat(unlocks).hasSize(1);
            assertThat(unlocks.get(0).level()).isEqualTo(UnlockLevel.FIRST_MESSAGE);
            // The third message, the first one from the other person.
            assertThat(unlocks.get(0).afterMessageId()).isEqualTo(messages.get(2).id());
        }

        @Test
        void a_conversation_nobody_answered_has_nothing_to_announce() {
            assertThat(UnlockLadder.unlocksOf(charla, conversation("AAAA"), after(Duration.ofHours(6))))
                    .isEmpty();
        }

        @Test
        void an_hour_going_by_with_nobody_writing_also_opens_level_two() {
            // Three turns each within the first minutes, then silence.
            List<Message> messages = conversation("ABABAB");

            var soon = UnlockLadder.unlocksOf(charla, messages, after(Duration.ofMinutes(30)));
            assertThat(soon).extracting(UnlockLadder.Unlock::level)
                    .containsExactly(UnlockLevel.FIRST_MESSAGE);

            var later = UnlockLadder.unlocksOf(charla, messages, after(Duration.ofHours(2)));
            assertThat(later).extracting(UnlockLadder.Unlock::level)
                    .containsExactly(UnlockLevel.FIRST_MESSAGE, UnlockLevel.CONVERSATION);
            // Nobody wrote it: time alone opened it.
            assertThat(later.get(1).afterMessageId()).isNull();
        }

        @Test
        void the_photo_is_announced_at_the_moment_the_second_person_accepted() {
            Instant first = after(Duration.ofHours(4));
            Instant second = after(Duration.ofHours(6));
            Conversation both =
                    charla.withPhotoWantedBy(ANA, first).withPhotoWantedBy(LEO, second);

            var unlocks =
                    UnlockLadder.unlocksOf(both, conversation("ABABAB"), after(Duration.ofHours(7)));

            assertThat(unlocks).extracting(UnlockLadder.Unlock::level)
                    .contains(UnlockLevel.GOOD_CONNECTION);
            assertThat(unlocks.get(unlocks.size() - 1).at()).isEqualTo(second);
        }

        @Test
        void the_same_conversation_always_gives_the_same_notices() {
            List<Message> messages = conversation("ABABAB");
            Instant now = after(Duration.ofHours(3));

            assertThat(UnlockLadder.unlocksOf(charla, messages, now))
                    .isEqualTo(UnlockLadder.unlocksOf(charla, messages, now));
        }
    }
}
