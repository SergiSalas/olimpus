package com.sergisalas.olimpus.matching.domain;

public enum ConversationState {
    /** Live: messages can be written until closing time. */
    OPEN,
    /** Closed in silence at midday, and both went back into the round. */
    CANCELLED,
    /** 22:00 arrived and there was no mutual yes. Nothing more to write. */
    CLOSED,
    /** Someone reported or blocked: cut on the spot, and they never meet again. */
    BLOCKED,
    /**
     * Both said yes at the end of the day. The chat stays open with no closing
     * time, and it does not take up the one new conversation a day: connections
     * pile up, new people keep arriving.
     */
    CONNECTED
}
