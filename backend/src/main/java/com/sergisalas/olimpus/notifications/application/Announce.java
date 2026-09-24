package com.sergisalas.olimpus.notifications.application;

import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.notifications.domain.Notice;
import com.sergisalas.olimpus.notifications.domain.Notifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The five moments Olimpus speaks, and what it says at each.
 *
 * <p>Wording lives outside, in messages.properties, like every other text a
 * person reads, and is picked in each phone's language when sending. What lives
 * here is the rule: who gets told, and when.
 */
public class Announce {

    private final Notifier notifier;

    public Announce(Notifier notifier) {
        this.notifier = notifier;
    }

    /** 8:00, or whenever the round handed out conversations. */
    public void newMatches(List<Conversation> conversations) {
        List<Notice> notices = new ArrayList<>();
        for (Conversation conversation : conversations) {
            for (UUID account : List.of(conversation.accountA(), conversation.accountB())) {
                notices.add(
                        new Notice(
                                account,
                                Notice.Kind.NEW_MATCH,
                                "notice.new-match.title",
                                "notice.new-match.body",
                                "/today"));
            }
        }
        notifier.send(notices);
    }

    /** The second chance found someone for people who had nobody. */
    public void secondChance(List<Conversation> conversations) {
        List<Notice> notices = new ArrayList<>();
        for (Conversation conversation : conversations) {
            for (UUID account : List.of(conversation.accountA(), conversation.accountB())) {
                notices.add(
                        new Notice(
                                account,
                                Notice.Kind.SECOND_CHANCE,
                                "notice.second-chance.title",
                                "notice.second-chance.body",
                                "/today"));
            }
        }
        notifier.send(notices);
    }

    /**
     * Someone wrote. Only to the person who did not write it, and the text never
     * carries the message: a notification can be read by whoever holds the phone.
     */
    public void newMessage(Conversation conversation, UUID sender) {
        notifier.send(
                List.of(
                        new Notice(
                                conversation.partnerOf(sender),
                                Notice.Kind.NEW_MESSAGE,
                                "notice.new-message.title",
                                "notice.new-message.body",
                                "/chat/" + conversation.id())));
    }

    /** 21:30: half an hour left, and the question of the day. */
    public void closingSoon(List<Conversation> conversations) {
        List<Notice> notices = new ArrayList<>();
        for (Conversation conversation : conversations) {
            for (UUID account : List.of(conversation.accountA(), conversation.accountB())) {
                notices.add(
                        new Notice(
                                account,
                                Notice.Kind.CLOSING_SOON,
                                "notice.closing-soon.title",
                                "notice.closing-soon.body",
                                "/chat/" + conversation.id()));
            }
        }
        notifier.send(notices);
    }

    /**
     * 22:00, and only to those who connected. Whoever did not is told nothing:
     * silence says less than "it did not work out", and says nothing about what
     * the other person answered.
     */
    public void connections(List<Conversation> conversations) {
        List<Notice> notices = new ArrayList<>();
        for (Conversation conversation : conversations) {
            for (UUID account : List.of(conversation.accountA(), conversation.accountB())) {
                notices.add(
                        new Notice(
                                account,
                                Notice.Kind.CONNECTION,
                                "notice.connection.title",
                                "notice.connection.body",
                                "/chat/" + conversation.id()));
            }
        }
        notifier.send(notices);
    }
}
