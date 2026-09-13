package com.sergisalas.olimpus.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.matching.domain.Gente;
import com.sergisalas.olimpus.profile.domain.Profile;
import org.junit.jupiter.api.Test;

class IcebreakersTest {

    @Test
    void la_pregunta_sale_de_algo_que_los_dos_han_puesto() {
        Profile ana = Gente.persona().intereses("escalada", "cine", "vinos", "teatro", "correr").build();
        Profile leo = Gente.persona().intereses("escalada", "surf", "ajedrez", "arte", "leer").build();

        assertThat(Icebreakers.forPair(ana, leo))
                .isEqualTo("Los dos habéis puesto escalada: ¿montaña o rocódromo?");
    }

    @Test
    void entre_varios_intereses_comunes_gana_el_mas_raro() {
        // "viajar" lo pone medio mundo; "kendo", casi nadie. Preguntar por lo
        // comun no da conversacion.
        Profile ana = Gente.persona().intereses("viajar", "cine", "kendo", "musica", "series").build();
        Profile leo = Gente.persona().intereses("viajar", "cine", "kendo", "leer", "correr").build();

        assertThat(Icebreakers.rarestShared(ana, leo)).contains("kendo");
        assertThat(Icebreakers.forPair(ana, leo)).contains("kendo");
    }

    @Test
    void los_dos_reciben_exactamente_la_misma_pregunta() {
        Profile ana = Gente.persona().intereses("astronomia", "cine", "vinos", "teatro", "correr").build();
        Profile leo = Gente.persona().intereses("astronomia", "cine", "surf", "arte", "leer").build();

        assertThat(Icebreakers.forPair(ana, leo)).isEqualTo(Icebreakers.forPair(leo, ana));
    }

    @Test
    void sin_nada_en_comun_se_pregunta_igualmente_algo() {
        Profile ana = Gente.persona().intereses("kendo", "apicultura", "ceramica", "buceo", "luthier").build();
        Profile leo = Gente.persona().intereses("cine", "correr", "vinos", "arte", "leer").build();

        assertThat(Icebreakers.rarestShared(ana, leo)).isEmpty();
        assertThat(Icebreakers.forPair(ana, leo)).contains("¿qué es lo último que te ha enganchado?");
    }

    @Test
    void todos_los_intereses_del_catalogo_tienen_su_pregunta() {
        for (var entrada : com.sergisalas.olimpus.profile.domain.InterestCatalog.ENTRIES) {
            Profile a = soloConEste(entrada.name(), "cine", "leer", "correr", "surf", "arte");
            Profile b = soloConEste(entrada.name(), "vinos", "teatro", "bailar", "surf", "arte");

            assertThat(Icebreakers.forPair(a, b))
                    .as("interes %s", entrada.name())
                    .doesNotContain("¿cómo empezaste?");
        }
    }

    /** El interes a probar mas relleno, sin repetirlo si ya estaba en el relleno. */
    private static Profile soloConEste(String interes, String... relleno) {
        var lista = new java.util.LinkedHashSet<String>();
        lista.add(interes);
        for (String otro : relleno) {
            if (lista.size() < 5) lista.add(otro);
        }
        return Gente.persona().intereses(lista.toArray(String[]::new)).build();
    }

    @Test
    void el_guion_de_los_intereses_no_se_ve_en_la_pregunta() {
        Profile a = Gente.persona().intereses("juegos-de-mesa", "cine", "leer", "correr", "surf").build();
        Profile b = Gente.persona().intereses("juegos-de-mesa", "arte", "vinos", "teatro", "bailar").build();

        assertThat(Icebreakers.forPair(a, b)).contains("juegos de mesa").doesNotContain("juegos-de-mesa");
    }
}
