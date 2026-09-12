package com.sergisalas.olimpus.matching.application;

import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.PartnerView;
import com.sergisalas.olimpus.matching.domain.ProfileDirectory;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: ¿con quien hablo hoy?
 *
 * <p>Devuelve solo lo que el nivel de desbloqueo permite ver. Si todavia no hay
 * conversacion, dice a que hora llega la siguiente, que es justo lo que evita
 * que la espera se viva como que la app no funciona.
 */
public class GetTodaysConversation {

    public record Today(
            Optional<Conversation> conversation,
            Optional<PartnerView> partner,
            List<String> sharedInterests,
            LocalDate date) {

        public static Today nothing(LocalDate date) {
            return new Today(Optional.empty(), Optional.empty(), List.of(), date);
        }
    }

    private final ConversationRepository conversations;
    private final ProfileDirectory profiles;
    private final RoundSchedule schedule;
    private final Clock clock;

    public GetTodaysConversation(
            ConversationRepository conversations,
            ProfileDirectory profiles,
            RoundSchedule schedule,
            Clock clock) {
        this.conversations = conversations;
        this.profiles = profiles;
        this.schedule = schedule;
        this.clock = clock;
    }

    public Today execute(UUID accountId) {
        LocalDate today = schedule.dateOf(clock.instant());

        Optional<Conversation> abierta = conversations.openFor(accountId, today);
        if (abierta.isEmpty()) {
            return Today.nothing(today);
        }

        Conversation conversation = abierta.get();
        Optional<Profile> yo = profiles.byAccountId(accountId);
        Optional<Profile> otro = profiles.byAccountId(conversation.partnerOf(accountId));
        if (yo.isEmpty() || otro.isEmpty()) {
            return Today.nothing(today);
        }

        List<String> comunes = PartnerView.sharedInterests(yo.get(), otro.get());
        PartnerView vista = PartnerView.levelZero(otro.get(), yo.get(), today, comunes);

        return new Today(Optional.of(conversation), Optional.of(vista), comunes, today);
    }
}
