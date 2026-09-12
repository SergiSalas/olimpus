package com.sergisalas.olimpus.config;

import com.sergisalas.olimpus.auth.application.AuthenticateSession;
import com.sergisalas.olimpus.auth.application.RequestLoginCode;
import com.sergisalas.olimpus.auth.application.VerifyLoginCode;
import com.sergisalas.olimpus.auth.domain.AccountRepository;
import com.sergisalas.olimpus.auth.domain.CodeSender;
import com.sergisalas.olimpus.auth.domain.LoginCodeRepository;
import com.sergisalas.olimpus.auth.domain.Secrets;
import com.sergisalas.olimpus.auth.domain.SessionRepository;
import com.sergisalas.olimpus.health.application.CheckHealth;
import com.sergisalas.olimpus.health.domain.DatabaseInfo;
import com.sergisalas.olimpus.shared.domain.Hasher;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Aqui se montan a mano los casos de uso. Es el unico sitio donde Spring
 * y el dominio se tocan: asi el dominio sigue sin depender de nada.
 */
@Configuration
public class DomainBeans {

    /** Todo el proyecto trabaja en UTC. Un solo reloj, y en los tests se sustituye. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CheckHealth checkHealth(DatabaseInfo databaseInfo) {
        return new CheckHealth(databaseInfo);
    }

    @Bean
    RequestLoginCode requestLoginCode(
            LoginCodeRepository codes,
            Secrets secrets,
            Hasher hasher,
            CodeSender sender,
            Clock clock) {
        return new RequestLoginCode(codes, secrets, hasher, sender, clock);
    }

    @Bean
    VerifyLoginCode verifyLoginCode(
            LoginCodeRepository codes,
            AccountRepository accounts,
            SessionRepository sessions,
            Secrets secrets,
            Hasher hasher,
            Clock clock) {
        return new VerifyLoginCode(codes, accounts, sessions, secrets, hasher, clock);
    }

    @Bean
    AuthenticateSession authenticateSession(
            SessionRepository sessions, AccountRepository accounts, Hasher hasher, Clock clock) {
        return new AuthenticateSession(sessions, accounts, hasher, clock);
    }
}
