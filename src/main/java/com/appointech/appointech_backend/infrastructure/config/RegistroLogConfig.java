package com.appointech.appointech_backend.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Hilo único con cola acotada para guardar logs sin ocupar una conexión extra por cada petición. */
@Configuration
public class RegistroLogConfig {

    /**
     * Si la cola se llena, el hilo que registra lo guarda él mismo (CallerRuns): se frena a quien genera logs en vez
     * de perderlos. Al apagar la aplicación, {@code close()} espera a vaciar lo pendiente.
     */
    @Bean(name = "registroLogExecutor", destroyMethod = "close")
    public ExecutorService registroLogExecutor(@Value("${registro-log.cola-maxima:5000}") int colaMaxima) {
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(colaMaxima),
                tarea -> {
                    Thread hilo = new Thread(tarea, "registro-log");
                    hilo.setDaemon(true);
                    return hilo;
                },
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
