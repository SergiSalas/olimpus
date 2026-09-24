package com.sergisalas.olimpus.safety.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sergisalas.olimpus.chat.domain.NotYourConversationException;
import com.sergisalas.olimpus.matching.application.FakeRoundWorld;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationState;
import com.sergisalas.olimpus.matching.domain.Filters;
import com.sergisalas.olimpus.matching.domain.MatchContext;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import com.sergisalas.olimpus.matching.domain.TestPeople;
import com.sergisalas.olimpus.profile.domain.Gender;
import com.sergisalas.olimpus.profile.domain.Profile;
import com.sergisalas.olimpus.safety.domain.Block;
import com.sergisalas.olimpus.safety.domain.BlockRepository;
import com.sergisalas.olimpus.safety.domain.ReportReason;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Reporting has to work on the worst day of someone's week. These tests care
 * about one thing: that the escape hatch is immediate and that it holds.
 */
class ReportAndBlockTest {

    private FakeRoundWorld world;
    private List<Block> blocks;
    private ReportAndBlock report;
    private Profile ana;
    private Profile leo;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        world = new FakeRoundWorld();
        blocks = new ArrayList<>();

        ana = TestPeople.person().nickname("Ana").gender(Gender.WOMAN).seeking(Gender.MAN).build();
        leo = TestPeople.person().nickname("Leo").gender(Gender.MAN).seeking(Gender.WOMAN).build();
        world.people.addAll(List.of(ana, leo));

        conversationId =
                world.dailyRound().execute(TestPeople.TODAY, RoundKind.MAIN).created().get(0).id();

        BlockRepository repository =
                new BlockRepository() {
                    @Override
                    public void save(Block block) {
                        blocks.add(block);
                    }

                    @Override
                    public List<Block> byBlocker(UUID blocker) {
                        return blocks.stream().filter(b -> b.blocker().equals(blocker)).toList();
                    }
                };

        report = new ReportAndBlock(world.conversations, repository, world.clock);
    }

    private Conversation current() {
        return world.conversations.byId(conversationId).orElseThrow();
    }

    @Test
    void reporting_cuts_the_conversation_on_the_spot() {
        report.execute(conversationId, ana.accountId(), ReportReason.DISRESPECT);

        assertThat(current().state()).isEqualTo(ConversationState.BLOCKED);
        assertThat(current().acceptsMessagesAt(world.now)).isFalse();
    }

    @Test
    void the_reason_is_kept_because_a_person_will_read_it_later() {
        report.execute(conversationId, ana.accountId(), ReportReason.LOOKS_UNDERAGE);

        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).reason()).isEqualTo(ReportReason.LOOKS_UNDERAGE);
        assertThat(blocks.get(0).isReport()).isTrue();
    }

    @Test
    void blocking_without_reporting_is_allowed_and_cuts_just_the_same() {
        report.execute(conversationId, ana.accountId(), null);

        assertThat(current().state()).isEqualTo(ConversationState.BLOCKED);
        assertThat(blocks.get(0).isReport()).isFalse();
    }

    @Test
    void a_block_works_in_both_directions_and_never_gives_way() {
        report.execute(conversationId, ana.accountId(), ReportReason.SPAM);

        MatchContext blocked =
                MatchContext.on(TestPeople.TODAY)
                        .blocked(ana.accountId(), leo.accountId())
                        // Even after waiting a very long time.
                        .waiting(ana.accountId(), 500)
                        .waiting(leo.accountId(), 500)
                        .build();

        assertThat(Filters.passesHard(ana, leo, blocked)).isFalse();
        assertThat(Filters.passesHard(leo, ana, blocked)).isFalse();
    }

    @Test
    void the_two_never_get_matched_again_in_any_later_round() {
        report.execute(conversationId, ana.accountId(), ReportReason.DISRESPECT);
        world.blocked.add(com.sergisalas.olimpus.matching.domain.PairKey.of(ana.accountId(), leo.accountId()));

        var tomorrow = world.dailyRound().execute(TestPeople.TODAY.plusDays(1), RoundKind.MAIN);

        assertThat(tomorrow.created()).isEmpty();
        assertThat(tomorrow.leftOut()).hasSize(2);
    }

    @Test
    void an_outsider_cannot_report_a_conversation_that_is_not_theirs() {
        assertThatThrownBy(
                        () -> report.execute(conversationId, UUID.randomUUID(), ReportReason.SPAM))
                .isInstanceOf(NotYourConversationException.class);

        assertThat(blocks).isEmpty();
        assertThat(current().state()).isEqualTo(ConversationState.OPEN);
    }

    @Test
    void a_connection_can_be_reported_too_because_things_go_wrong_later() {
        world.conversations.save(current().connected());

        report.execute(conversationId, leo.accountId(), ReportReason.UNWANTED_SEXUAL);

        assertThat(current().state()).isEqualTo(ConversationState.BLOCKED);
        assertThat(current().acceptsMessagesAt(world.now)).isFalse();
    }
}
