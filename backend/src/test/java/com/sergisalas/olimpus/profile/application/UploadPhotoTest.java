package com.sergisalas.olimpus.profile.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sergisalas.olimpus.profile.domain.ModerationState;
import com.sergisalas.olimpus.profile.domain.Photo;
import com.sergisalas.olimpus.profile.domain.PhotoModerator;
import com.sergisalas.olimpus.profile.domain.PhotoNotVisibleException;
import com.sergisalas.olimpus.profile.domain.PhotoRepository;
import com.sergisalas.olimpus.profile.domain.PhotoStorage;
import com.sergisalas.olimpus.shared.domain.RuleViolationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UploadPhotoTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-09-13T10:00:00Z"), ZoneOffset.UTC);

    /** A real (tiny) JPEG: starts with the bytes every JPEG starts with. */
    private static byte[] jpeg(int size) {
        byte[] bytes = new byte[Math.max(8, size)];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;
        return bytes;
    }

    private static byte[] png() {
        return new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    }

    private Map<UUID, Photo> guardadas;
    private Map<String, byte[]> ficheros;
    private List<String> borrados;
    private ModerationState veredicto;

    private UploadPhoto subir;
    private ViewPhoto ver;

    @BeforeEach
    void setUp() {
        guardadas = new HashMap<>();
        ficheros = new LinkedHashMap<>();
        borrados = new ArrayList<>();
        veredicto = ModerationState.APPROVED;

        PhotoRepository repo =
                new PhotoRepository() {
                    @Override
                    public Optional<Photo> findByAccountId(UUID accountId) {
                        return Optional.ofNullable(guardadas.get(accountId));
                    }

                    @Override
                    public void save(Photo photo) {
                        guardadas.put(photo.accountId(), photo);
                    }
                };

        PhotoStorage storage =
                new PhotoStorage() {
                    @Override
                    public String store(byte[] bytes, String contentType) {
                        String id = "fichero-" + ficheros.size();
                        ficheros.put(id, bytes);
                        return id;
                    }

                    @Override
                    public Optional<byte[]> read(String storageId) {
                        return Optional.ofNullable(ficheros.get(storageId));
                    }

                    @Override
                    public void delete(String storageId) {
                        borrados.add(storageId);
                        ficheros.remove(storageId);
                    }
                };

        PhotoModerator moderator = (bytes, contentType) -> veredicto;

        subir = new UploadPhoto(repo, storage, moderator, CLOCK);
        ver = new ViewPhoto(repo, storage);
    }

    @Test
    void una_foto_valida_se_guarda_y_su_dueno_puede_verla() {
        Photo photo = subir.execute(ANA, jpeg(2048), "image/jpeg");

        assertThat(photo.moderation()).isEqualTo(ModerationState.APPROVED);
        assertThat(photo.sizeBytes()).isEqualTo(2048);
        assertThat(ver.ownPhoto(ANA).contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void tambien_vale_png() {
        assertThat(subir.execute(ANA, png(), "image/png").contentType()).isEqualTo("image/png");
    }

    @Test
    void cambiar_de_foto_borra_los_bytes_de_la_anterior() {
        subir.execute(ANA, jpeg(1024), "image/jpeg");
        String primera = guardadas.get(ANA).storageId();

        subir.execute(ANA, jpeg(2048), "image/jpeg");

        assertThat(borrados).containsExactly(primera);
        assertThat(ficheros).hasSize(1);
        assertThat(guardadas).hasSize(1);
    }

    @Test
    void una_foto_que_no_pasa_la_moderacion_no_llega_a_guardarse() {
        veredicto = ModerationState.REJECTED;

        assertThatThrownBy(() -> subir.execute(ANA, jpeg(1024), "image/jpeg"))
                .isInstanceOf(RuleViolationException.class);

        assertThat(guardadas).isEmpty();
        assertThat(ficheros).isEmpty();
    }

    @Test
    void mientras_la_moderacion_decide_la_foto_no_se_puede_ensenar_a_nadie() {
        veredicto = ModerationState.PENDING;

        Photo photo = subir.execute(ANA, jpeg(1024), "image/jpeg");

        assertThat(photo.canBeShown()).isFalse();
        assertThat(ver.hasShowablePhoto(ANA)).isFalse();
        // Pero su dueno si la ve: tiene que saber que ha subido.
        assertThat(ver.ownPhoto(ANA).content()).isNotEmpty();
    }

    @Test
    void un_pdf_disfrazado_de_foto_no_cuela() {
        byte[] pdf = "%PDF-1.7 esto no es una foto".getBytes();

        assertThatThrownBy(() -> subir.execute(ANA, pdf, "image/jpeg"))
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("photo.not-an-image");
    }

    @Test
    void un_png_declarado_como_jpeg_tampoco() {
        assertThatThrownBy(() -> subir.execute(ANA, png(), "image/jpeg"))
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("photo.not-an-image");
    }

    @Test
    void un_tipo_que_no_admitimos_se_rechaza_antes_de_mirar_nada() {
        assertThatThrownBy(() -> subir.execute(ANA, jpeg(1024), "image/gif"))
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("photo.type.unsupported");
    }

    @Test
    void una_foto_de_mas_de_cinco_megas_se_rechaza() {
        assertThatThrownBy(() -> subir.execute(ANA, jpeg((int) Photo.MAX_BYTES + 1), "image/jpeg"))
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("photo.too-big");

        assertThat(ficheros).isEmpty();
    }

    @Test
    void un_fichero_vacio_se_rechaza() {
        assertThatThrownBy(() -> subir.execute(ANA, new byte[0], "image/jpeg"))
                .isInstanceOf(RuleViolationException.class);
    }

    @Test
    void quien_no_ha_subido_foto_no_tiene_nada_que_ver() {
        assertThatThrownBy(() -> ver.ownPhoto(ANA)).isInstanceOf(PhotoNotVisibleException.class);
        assertThat(ver.hasShowablePhoto(ANA)).isFalse();
    }
}
