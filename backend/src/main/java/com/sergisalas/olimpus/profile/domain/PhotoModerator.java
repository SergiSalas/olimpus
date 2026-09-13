package com.sergisalas.olimpus.profile.domain;

/**
 * Port: deciding whether a photo can be shown at all.
 *
 * <p>The other swappable piece. While developing, a version that approves
 * everything and says so in the log. <b>The beta does not open without a real
 * one</b>: this is an app that ends up showing photos between strangers.
 */
public interface PhotoModerator {

    ModerationState review(byte[] bytes, String contentType);
}
