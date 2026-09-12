package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.LocalDate;
import java.util.List;

/**
 * Puerto: reunir la historia que el reparto necesita (bloqueos, quien hablo con
 * quien y cuando, cuanto lleva esperando cada uno).
 *
 * <p>Vive detras de un puerto porque son varias consultas a la base de datos, y
 * el reparto tiene que poder probarse sin ninguna.
 */
public interface MatchContextFactory {

    MatchContext forRound(LocalDate today, List<Profile> pool);
}
