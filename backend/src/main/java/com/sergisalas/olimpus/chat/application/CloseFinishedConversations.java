package com.sergisalas.olimpus.chat.application;

import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Caso de uso: las 22:00.
 *
 * <p>Cierra las conversaciones a las que se les ha pasado la hora. A partir de
 * aqui manda la decision de cada uno, que llega en el paso 7.
 */
public class CloseFinishedConversations {

    private final ConversationRepository conversations;
    private final RoundSchedule schedule;
    private final Clock clock;

    public CloseFinishedConversations(
            ConversationRepository conversations, RoundSchedule schedule, Clock clock) {
        this.conversations = conversations;
        this.schedule = schedule;
        this.clock = clock;
    }

    public List<Conversation> execute() {
        var ahora = clock.instant();
        LocalDate hoy = schedule.dateOf(ahora);

        List<Conversation> cerradas = new ArrayList<>();
        // Se mira tambien el dia anterior: si el servidor estuvo caido a las
        // 22:00, las de ayer no pueden quedarse abiertas para siempre.
        for (LocalDate dia : List.of(hoy.minusDays(1), hoy)) {
            for (Conversation conversation : conversations.byDate(dia)) {
                if (conversation.isOpen() && !ahora.isBefore(conversation.closesAt())) {
                    Conversation cerrada = conversation.closed();
                    conversations.save(cerrada);
                    cerradas.add(cerrada);
                }
            }
        }
        return cerradas;
    }
}
