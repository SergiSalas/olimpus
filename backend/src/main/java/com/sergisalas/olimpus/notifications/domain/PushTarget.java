package com.sergisalas.olimpus.notifications.domain;

/**
 * One phone to reach, and the language its app is in.
 *
 * @param language a language code like "es" or "en"
 */
public record PushTarget(String token, String language) {}
