package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.LocalDate;
import java.util.List;

/**
 * Port: gather the history matching needs (blocks, who talked to whom and when,
 * how long each person has been waiting).
 *
 * <p>It lives behind a port because it takes several database queries, and
 * matching has to be testable without any.
 */
public interface MatchContextFactory {

    MatchContext forRound(LocalDate today, List<Profile> pool);
}
