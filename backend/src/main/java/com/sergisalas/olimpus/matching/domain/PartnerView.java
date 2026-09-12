package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lo poco que se ve de la otra persona al empezar: el nivel 0 de la escalera.
 *
 * <p>Edad, dos intereses y a que distancia esta. Ni apodo, ni bio, ni foto: eso
 * llega cuando la conversacion se lo gana.
 *
 * <p>Que esto sea un objeto aparte y no el perfil recortado en la pantalla es
 * deliberado: asi no hay forma de que un descuido mande el perfil entero al
 * movil. Lo que no esta aqui no sale del servidor.
 */
public record PartnerView(int age, List<String> interestsShown, int approxDistanceKm, int level) {

    public static final int INTERESES_VISIBLES_AL_EMPEZAR = 2;

    /**
     * @param shownFirst intereses que conviene mostrar primero, normalmente los
     *     que los dos tienen en comun: son los que dan de que hablar
     */
    public static PartnerView levelZero(
            Profile partner, Profile viewer, LocalDate today, List<String> shownFirst) {

        List<String> visibles = new ArrayList<>();
        for (String interest : shownFirst) {
            if (partner.interests().contains(interest) && visibles.size() < INTERESES_VISIBLES_AL_EMPEZAR) {
                visibles.add(interest);
            }
        }
        for (String interest : partner.interests()) {
            if (visibles.size() >= INTERESES_VISIBLES_AL_EMPEZAR) break;
            if (!visibles.contains(interest)) visibles.add(interest);
        }

        int km = (int) Math.round(viewer.location().distanceKmTo(partner.location()));

        return new PartnerView(partner.ageOn(today), List.copyOf(visibles), km, 0);
    }

    /** Intereses que tienen los dos, que son los que sirven para arrancar. */
    public static List<String> sharedInterests(Profile a, Profile b) {
        return a.interests().stream().filter(b.interests()::contains).sorted().toList();
    }
}
