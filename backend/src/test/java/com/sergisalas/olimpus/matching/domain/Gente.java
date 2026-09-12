package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Gender;
import com.sergisalas.olimpus.profile.domain.Intent;
import com.sergisalas.olimpus.profile.domain.InterestCatalog;
import com.sergisalas.olimpus.profile.domain.LanguageSkill;
import com.sergisalas.olimpus.profile.domain.Location;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Fabrica de gente para los tests del reparto. */
public final class Gente {

    public static final LocalDate HOY = LocalDate.of(2026, 9, 12);

    /** Plaza Catalunya, Barcelona. */
    public static final Location CENTRO = Location.rounded(41.3874, 2.1686);

    private Gente() {}

    public static Builder persona() {
        return new Builder();
    }

    /** Alguien de 30 anos que busca a cualquiera y a quien todos le encajan. */
    public static Profile cualquiera() {
        return persona().build();
    }

    public static final class Builder {
        private UUID id = UUID.randomUUID();
        private String nickname = "Alguien";
        private int edad = 30;
        private Gender gender = Gender.MUJER;
        private Set<Gender> seeking = Set.of(Gender.MUJER, Gender.HOMBRE, Gender.NO_BINARIO, Gender.OTRO);
        private int ageMin = 18;
        private int ageMax = 99;
        private int maxDistanceKm = 50;
        private Location location = CENTRO;
        private List<LanguageSkill> languages =
                List.of(new LanguageSkill("es", LanguageSkill.Level.NATIVO));
        private int sociability = 3;
        private int conversationDepth = 3;
        private Intent intent = Intent.CITAS;
        private Set<String> interests = Set.of("cine", "leer", "correr", "surf", "vinos");

        public Builder id(UUID v) {
            id = v;
            return this;
        }

        public Builder nickname(String v) {
            nickname = v;
            return this;
        }

        public Builder edad(int v) {
            edad = v;
            return this;
        }

        public Builder gender(Gender v) {
            gender = v;
            return this;
        }

        public Builder busca(Gender... v) {
            seeking = Set.of(v);
            return this;
        }

        public Builder edades(int min, int max) {
            ageMin = min;
            ageMax = max;
            return this;
        }

        public Builder distancia(int km) {
            maxDistanceKm = km;
            return this;
        }

        public Builder en(double lat, double lon) {
            location = Location.rounded(lat, lon);
            return this;
        }

        public Builder idiomas(LanguageSkill... v) {
            languages = List.of(v);
            return this;
        }

        public Builder sociabilidad(int v) {
            sociability = v;
            return this;
        }

        public Builder profundidad(int v) {
            conversationDepth = v;
            return this;
        }

        public Builder busca(Intent v) {
            intent = v;
            return this;
        }

        public Builder intereses(String... v) {
            interests = Set.of(v);
            return this;
        }

        public Profile build() {
            return new Profile(
                    id,
                    nickname,
                    "",
                    HOY.minusYears(edad).minusDays(1),
                    gender,
                    seeking,
                    ageMin,
                    ageMax,
                    maxDistanceKm,
                    location,
                    languages,
                    sociability,
                    conversationDepth,
                    intent,
                    interests);
        }
    }

    /**
     * Una poblacion variada y reproducible: edades, generos, sociabilidad e
     * intereses repartidos. Sirve para probar el reparto a escala.
     */
    public static List<Profile> poblacion(int cuantos, long semilla) {
        Random rng = new Random(semilla);
        Gender[] generos = {Gender.MUJER, Gender.HOMBRE, Gender.NO_BINARIO};
        Intent[] intenciones = {Intent.AMISTAD, Intent.CITAS, Intent.PAREJA, Intent.CASUAL};
        List<String> catalogo =
                InterestCatalog.ENTRIES.stream().map(InterestCatalog.Entry::name).toList();

        List<Profile> gente = new ArrayList<>();
        for (int i = 0; i < cuantos; i++) {
            Set<String> intereses = new LinkedHashSet<>();
            while (intereses.size() < 5 + rng.nextInt(4)) {
                intereses.add(catalogo.get(rng.nextInt(catalogo.size())));
            }
            gente.add(
                    persona()
                            .nickname("P" + i)
                            .edad(20 + rng.nextInt(25))
                            .gender(generos[rng.nextInt(generos.length)])
                            .edades(18, 99)
                            .distancia(50)
                            .en(41.3874 + (rng.nextDouble() - 0.5) * 0.1, 2.1686 + (rng.nextDouble() - 0.5) * 0.1)
                            .sociabilidad(1 + rng.nextInt(5))
                            .profundidad(1 + rng.nextInt(5))
                            .busca(intenciones[rng.nextInt(intenciones.length)])
                            .intereses(intereses.toArray(String[]::new))
                            .build());
        }
        return gente;
    }
}
