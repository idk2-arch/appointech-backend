package com.appointech.appointech_backend.domain.ports.in;

public interface LimpiarLogsAntiguosUseCase {
    int limpiar(int diasDeRetencion);
}
