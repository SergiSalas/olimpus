package com.sergisalas.olimpus.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sergisalas.olimpus.chat.domain.ChatClosedException;
import com.sergisalas.olimpus.chat.domain.Message;
import com.sergisalas.olimpus.chat.domain.MessageRepository;
import com.sergisalas.olimpus.chat.domain.NotYourConversationException;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.ConversationState;
import com.sergisalas.olimpus.matching.domain.Origin;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SendMessageTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID LEO = UUID.randomUUID();
    private static final UUID EXTRAÑO = UUID.randomUUID();

    private static final Instant MEDIODIA = Instant.parse("2026-09-13T10:00:00Z");
    private static final Instant CIERRE = Instant.parse("2026-09-13T20:00:00Z");

    private Instant ahora = MEDIODIA;
    private final Map<UUID, Conversation> conversaciones = new LinkedHashMap<>();
    private final List<Message> mensajes = new ArrayList<>();

    private Conversation charla;
    private SendMessage escribir;

    @BeforeEach
    void setUp() {
        charla =
                new Conversation(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 9, 13),
                        RoundKind.PRINCIPAL,
                        ANA,
                        LEO,
                        Origin.MEJOR_PAREJA,
                        0.8,
                        Instant.parse("2026-09-13T02:00:00Z"),
                        CIERRE,
                        ConversationState.ABIERTA,
                        0,
                        0,
                        "Los dos habéis puesto escalada: ¿montaña o rocódromo?");
        conversaciones.put(charla.id(), charla);

        ConversationRepository repoConversaciones =
                new ConversationRepository() {
                    @Override
                    public void save(Conversation conversation) {
                        conversaciones.put(conversation.id(), conversation);
                    }

                    @Override
                    public List<Conversation> byDate(LocalDate date) {
                        return conversaciones.values().stream()
                                .filter(c -> c.roundDate().equals(date))
                                .toList();
                    }

                    @Override
                    public Optional<Conversation> openFor(UUID accountId, LocalDate date) {
                        return conversaciones.values().stream()
                                .filter(c -> c.isOpen() && c.involves(accountId))
                                .findFirst();
                    }

                    @Override
                    public Optional<Conversation> byId(UUID id) {
                        return Optional.ofNullable(conversaciones.get(id));
                    }
                };

        MessageRepository repoMensajes =
                new MessageRepository() {
                    @Override
                    public void save(Message message) {
                        mensajes.add(message);
                    }

                    @Override
                    public List<Message> byConversation(UUID conversationId) {
                        return mensajes.stream()
                                .filter(m -> m.conversationId().equals(conversationId))
                                .toList();
                    }
                };

        Clock reloj =
                new Clock() {
                    @Override
                    public java.time.ZoneId getZone() {
                        return ZoneOffset.UTC;
                    }

                    @Override
                    public Clock withZone(java.time.ZoneId zone) {
                        return this;
                    }

                    @Override
                    public Instant instant() {
                        return ahora;
                    }
                };

        escribir = new SendMessage(repoConversaciones, repoMensajes, reloj);
    }

    @Test
    void escribir_guarda_el_mensaje_y_suma_a_la_cuenta_de_su_lado() {
        var enviado = escribir.execute(charla.id(), ANA, "Rocódromo, casi siempre");

        assertThat(mensajes).hasSize(1);
        assertThat(enviado.conversation().messagesFromA()).isEqualTo(1);
        assertThat(enviado.conversation().messagesFromB()).isZero();
        assertThat(enviado.conversation().bothHaveWritten()).isFalse();
    }

    @Test
    void la_conversacion_arranca_cuando_han_escrito_los_dos() {
        escribir.execute(charla.id(), ANA, "¡Hola!");
        escribir.execute(charla.id(), ANA, "¿Qué tal?");
        var despues = escribir.execute(charla.id(), LEO, "Buenas");

        assertThat(despues.conversation().messagesFromA()).isEqualTo(2);
        assertThat(despues.conversation().messagesFromB()).isEqualTo(1);
        assertThat(despues.conversation().bothHaveWritten()).isTrue();
        assertThat(despues.conversation().isSilent()).isFalse();
    }

    @Test
    void un_desconocido_no_puede_escribir_en_una_conversacion_ajena() {
        assertThatThrownBy(() -> escribir.execute(charla.id(), EXTRAÑO, "hola?"))
                .isInstanceOf(NotYourConversationException.class);

        assertThat(mensajes).isEmpty();
    }

    @Test
    void una_conversacion_que_no_existe_se_trata_igual_que_una_ajena() {
        assertThatThrownBy(() -> escribir.execute(UUID.randomUUID(), ANA, "hola?"))
                .isInstanceOf(NotYourConversationException.class);
    }

    @Test
    void a_las_diez_de_la_noche_ya_no_se_puede_escribir() {
        ahora = CIERRE;

        assertThatThrownBy(() -> escribir.execute(charla.id(), ANA, "¿sigues ahí?"))
                .isInstanceOf(ChatClosedException.class);

        assertThat(mensajes).isEmpty();
    }

    @Test
    void en_una_conversacion_cancelada_tampoco() {
        conversaciones.put(charla.id(), charla.cancelled());

        assertThatThrownBy(() -> escribir.execute(charla.id(), ANA, "hola"))
                .isInstanceOf(ChatClosedException.class);
    }

    @Test
    void un_mensaje_vacio_o_de_solo_espacios_no_cuenta() {
        assertThatThrownBy(() -> escribir.execute(charla.id(), ANA, "   "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(mensajes).isEmpty();
        assertThat(conversaciones.get(charla.id()).messagesFromA()).isZero();
    }

    @Test
    void un_mensaje_kilometrico_se_rechaza() {
        assertThatThrownBy(() -> escribir.execute(charla.id(), ANA, "x".repeat(1001)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
