package com.sergisalas.olimpus.chat.domain;

/**
 * Port: deciding whether a message can be sent at all.
 *
 * <p>The design document asks for moderation <b>as it happens</b>, not a review
 * afterwards: a message that has already been read cannot be unread. So this
 * sits in the path of every message, before it is stored.
 *
 * <p>While developing, a version that lets everything through and says so. The
 * beta does not open with that one in place.
 */
public interface MessageModerator {

    enum Verdict {
        ALLOW,
        /** Not sent, and the person is told so. */
        REJECT
    }

    Verdict review(String text);
}
