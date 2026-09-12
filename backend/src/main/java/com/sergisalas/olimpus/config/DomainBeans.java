package com.sergisalas.olimpus.config;

import com.sergisalas.olimpus.health.application.CheckHealth;
import com.sergisalas.olimpus.health.domain.DatabaseInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Aqui se montan a mano los casos de uso. Es el unico sitio donde Spring
 * y el dominio se tocan: asi el dominio sigue sin depender de nada.
 */
@Configuration
public class DomainBeans {

    @Bean
    CheckHealth checkHealth(DatabaseInfo databaseInfo) {
        return new CheckHealth(databaseInfo);
    }
}
