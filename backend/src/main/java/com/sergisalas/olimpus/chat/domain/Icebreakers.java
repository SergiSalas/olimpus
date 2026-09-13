package com.sergisalas.olimpus.chat.domain;

import com.sergisalas.olimpus.profile.domain.InterestCatalog;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * La pregunta con la que arranca la conversacion.
 *
 * <p>El momento mas dificil de una app de chat es el primer mensaje. Aqui nadie
 * tiene que inventarse nada: la app pone una pregunta concreta sacada de algo
 * que los dos han puesto.
 *
 * <p>Se elige el interes compartido <b>mas raro</b>, no el primero: que los dos
 * hayan puesto "viajar" no da conversacion; que los dos hayan puesto "kendo",
 * si.
 */
public final class Icebreakers {

    private Icebreakers() {}

    private static final Map<String, String> PREGUNTAS =
            Map.ofEntries(
                    Map.entry("viajar", "¿el último sitio que te sorprendió?"),
                    Map.entry("musica", "¿qué estás escuchando estos días?"),
                    Map.entry("cine", "¿la última que te gustó de verdad?"),
                    Map.entry("cocinar", "¿tu plato de rescate cuando no hay tiempo?"),
                    Map.entry("series", "¿alguna que hayas visto dos veces?"),
                    Map.entry("leer", "¿qué tienes ahora en la mesilla?"),
                    Map.entry("deporte", "¿lo practicas o lo ves?"),
                    Map.entry("salir-de-tapas", "¿tu sitio de siempre?"),
                    Map.entry("fotografia", "¿móvil o cámara?"),
                    Map.entry("senderismo", "¿la mejor ruta que has hecho?"),
                    Map.entry("conciertos", "¿el último al que fuiste?"),
                    Map.entry("videojuegos", "¿de los de perderse horas o de partida corta?"),
                    Map.entry("arte", "¿alguna exposición que te dejara tocado?"),
                    Map.entry("correr", "¿de mañana o de noche?"),
                    Map.entry("gimnasio", "¿por salud o porque te engancha?"),
                    Map.entry("animales", "¿tienes alguno?"),
                    Map.entry("teatro", "¿la última obra que viste?"),
                    Map.entry("jardineria", "¿qué tienes plantado ahora?"),
                    Map.entry("podcasts", "¿cuál recomiendas sin dudar?"),
                    Map.entry("bailar", "¿de academia o de fiesta?"),
                    Map.entry("tocar-guitarra", "¿desde cuándo tocas?"),
                    Map.entry("surf", "¿dónde sueles ir?"),
                    Map.entry("ciclismo", "¿carretera o montaña?"),
                    Map.entry("historia", "¿qué época te tira más?"),
                    Map.entry("astronomia", "¿has visto algo con telescopio?"),
                    Map.entry("escalada", "¿montaña o rocódromo?"),
                    Map.entry("ajedrez", "¿juegas online o en tablero?"),
                    Map.entry("ceramica", "¿torno o a mano?"),
                    Map.entry("buceo", "¿la mejor inmersión que has hecho?"),
                    Map.entry("juegos-de-mesa", "¿cuál pones cuando viene gente?"),
                    Map.entry("filosofia", "¿algún autor que te haya cambiado algo?"),
                    Map.entry("vinos", "¿tinto de invierno o blanco de terraza?"),
                    Map.entry("improvisacion", "¿haces bolos o es de puertas adentro?"),
                    Map.entry("apicultura", "¿tienes colmenas de verdad?"),
                    Map.entry("escalada-en-hielo", "¿dónde se hace eso por aquí?"),
                    Map.entry("luthier", "¿qué instrumento construyes?"),
                    Map.entry("birdwatching", "¿el mejor avistamiento que recuerdas?"),
                    Map.entry("kendo", "¿cuánto llevas practicando?"));

    /** Cuando no comparten nada: se pregunta por lo raro que tenga el otro. */
    private static final String SIN_NADA_EN_COMUN =
            "No tenéis intereses en común, así que empezad por ahí: ¿qué es lo último que te ha enganchado?";

    public static String forPair(Profile a, Profile b) {
        Optional<String> compartido = rarestShared(a, b);

        if (compartido.isEmpty()) {
            return SIN_NADA_EN_COMUN;
        }

        String interes = compartido.get();
        String pregunta = PREGUNTAS.getOrDefault(interes, "¿cómo empezaste?");
        return "Los dos habéis puesto " + legible(interes) + ": " + pregunta;
    }

    /**
     * El interes compartido menos comun. Al desempatar por nombre, dos personas
     * siempre reciben la misma pregunta.
     */
    public static Optional<String> rarestShared(Profile a, Profile b) {
        List<String> compartidos =
                a.interests().stream().filter(b.interests()::contains).sorted().toList();

        return compartidos.stream()
                .min(Comparator.comparingDouble(InterestCatalog::popularityOf));
    }

    private static String legible(String interes) {
        return interes.replace('-', ' ');
    }
}
