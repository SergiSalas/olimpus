package com.sergisalas.olimpus.safety.domain;

/**
 * Why someone is being reported.
 *
 * <p>The list is short on purpose: a person in the middle of something
 * unpleasant is not going to read fifteen options. Anything that does not fit
 * goes in {@link #OTHER} with the conversation attached, which is what a human
 * will actually read.
 */
public enum ReportReason {
    /** Insults, contempt, pushing. */
    DISRESPECT,
    /** Sexual content that was not asked for. */
    UNWANTED_SEXUAL,
    /** Selling something, links, the same message to everyone. */
    SPAM,
    /** The person does not seem to be who they say. */
    FAKE_PROFILE,
    /** Looks under 18. Goes to the top of the pile. */
    LOOKS_UNDERAGE,
    OTHER
}
