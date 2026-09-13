package com.sergisalas.olimpus.chat.adapter.in;

import com.sergisalas.olimpus.chat.application.CloseFinishedConversations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Las 22:00. Se comprueba cada cinco minutos en vez de una sola vez al dia: si
 * el servidor estaba caido justo a esa hora, las conversaciones no se quedan
 * abiertas hasta el dia siguiente.
 */
@Component
public class ClosingScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClosingScheduler.class);

    private final CloseFinishedConversations closeFinished;

    public ClosingScheduler(CloseFinishedConversations closeFinished) {
        this.closeFinished = closeFinished;
    }

    @Scheduled(fixedDelayString = "${olimpus.cierre.cada-ms:300000}")
    public void cerrarLasQueTocan() {
        var cerradas = closeFinished.execute();
        if (!cerradas.isEmpty()) {
            log.info("Cerradas {} conversaciones a las que se les paso la hora.", cerradas.size());
        }
    }
}
