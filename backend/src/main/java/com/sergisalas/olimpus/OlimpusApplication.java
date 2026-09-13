package com.sergisalas.olimpus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** The scheduled tasks are the two rounds of the day (4:00 and 14:00) and the 22:00 closing. */
@EnableScheduling
@SpringBootApplication
public class OlimpusApplication {

    public static void main(String[] args) {
        SpringApplication.run(OlimpusApplication.class, args);
    }
}
