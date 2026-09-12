package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Profile;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Cada interes pesa segun lo raro que sea en la gente que hay de verdad.
 *
 * <p>Que dos personas compartan "viajar" no dice nada: lo marca la mayoria.
 * Que compartan "escalada-en-hielo" es una senal enorme. Contar intereses
 * comunes sin pesarlos tira a la basura casi toda esa informacion.
 */
public final class InterestWeights {

    private final Map<String, Double> weights;

    private InterestWeights(Map<String, Double> weights) {
        this.weights = weights;
    }

    /**
     * Se calcula sobre la poblacion real, no sobre una tabla fija: en un campus
     * "escalada" puede ser comun y en otra ciudad rarisimo.
     */
    public static InterestWeights fromPopulation(Collection<Profile> population) {
        Map<String, Integer> counts = new HashMap<>();
        for (Profile p : population) {
            for (String interest : p.interests()) {
                counts.merge(interest, 1, Integer::sum);
            }
        }
        int n = population.size();
        Map<String, Double> weights = new HashMap<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            // +1 arriba y abajo para que un interes que nadie eligio no reviente
            // el logaritmo y para que el mas comun siga pesando algo.
            weights.put(entry.getKey(), Math.log((n + 1.0) / (entry.getValue() + 1.0)));
        }
        return new InterestWeights(weights);
    }

    /** Todos los intereses pesan igual. Solo para tests. */
    public static InterestWeights uniform() {
        return new InterestWeights(Map.of());
    }

    public double weight(String interest) {
        return weights.getOrDefault(interest, 1.0);
    }

    /**
     * Parecido entre dos listas de intereses, de 0 a 1.
     *
     * <p>Se divide por la raiz del producto de los dos totales (parecido tipo
     * coseno) para que alguien con ocho intereses no gane por volumen a alguien
     * con cinco.
     */
    public double similarity(Set<String> mine, Set<String> theirs) {
        double shared = 0;
        for (String interest : mine) {
            if (theirs.contains(interest)) {
                shared += weight(interest);
            }
        }
        if (shared == 0) return 0;

        double normMine = mine.stream().mapToDouble(this::weight).sum();
        double normTheirs = theirs.stream().mapToDouble(this::weight).sum();
        if (normMine == 0 || normTheirs == 0) return 0;

        return Math.min(1, shared / Math.sqrt(normMine * normTheirs));
    }
}
