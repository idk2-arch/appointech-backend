package com.appointech.appointech_backend.infrastructure.jobs;

import com.appointech.appointech_backend.domain.ports.in.LimpiarLogsAntiguosUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LimpiezaLogsJob {

    private final LimpiarLogsAntiguosUseCase limpiarLogsAntiguosUseCase;
    private final int diasDeRetencion;

    public LimpiezaLogsJob(LimpiarLogsAntiguosUseCase limpiarLogsAntiguosUseCase,
                           @Value("${logs.retencion-dias:30}") int diasDeRetencion) {
        this.limpiarLogsAntiguosUseCase = limpiarLogsAntiguosUseCase;
        this.diasDeRetencion = diasDeRetencion;
    }

    @Scheduled(cron = "${logs.limpieza-cron:0 0 3 * * *}")
    public void ejecutar() {
        limpiarLogsAntiguosUseCase.limpiar(diasDeRetencion);
    }
}
