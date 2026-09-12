package com.sergisalas.olimpus.health.adapter.in;

import com.sergisalas.olimpus.health.application.CheckHealth;
import com.sergisalas.olimpus.health.domain.Health;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador de entrada: expone el caso de uso por HTTP. */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final CheckHealth checkHealth;

    public HealthController(CheckHealth checkHealth) {
        this.checkHealth = checkHealth;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        Health health = checkHealth.execute();
        return new HealthResponse("ok", health.schemaVersion(), health.databaseTime());
    }

    /** Lo que viaja al movil. Separado del dominio a proposito. */
    public record HealthResponse(String status, String schemaVersion, Instant databaseTime) {}
}
