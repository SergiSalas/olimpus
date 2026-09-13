package com.sergisalas.olimpus.matching.domain;

public enum ConversationState {
    /** Live: messages can be written until closing time. */
    OPEN,
    /** Closed in silence at midday, and both went back into the round. */
    CANCELLED,
    /** 22:00 arrived. From here on, each person's decision rules. */
    CLOSED
}
