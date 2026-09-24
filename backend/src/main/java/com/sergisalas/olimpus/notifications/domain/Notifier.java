package com.sergisalas.olimpus.notifications.domain;

import java.util.List;

/**
 * Port: getting a notice onto someone's phone.
 *
 * <p>Sending is best-effort by nature: a phone can be off, the token can be
 * stale, the push service can be down. Nothing in the app may depend on a notice
 * arriving, which is why this returns nothing and never throws.
 */
public interface Notifier {

    void send(List<Notice> notices);
}
