package com.sergisalas.olimpus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Las tareas programadas son las dos rondas del dia: las 4:00 y las 14:00. */
@EnableScheduling
@SpringBootApplication
public class OlimpusApplication {

    public static void main(String[] args) {
        SpringApplication.run(OlimpusApplication.class, args);
    }
}
