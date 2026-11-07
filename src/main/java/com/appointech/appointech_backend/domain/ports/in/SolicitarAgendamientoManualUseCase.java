package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Cita;

public interface SolicitarAgendamientoManualUseCase {
    /**
     * Solo permitido cuando la ubicación está FUERA_DE_COBERTURA o cuando cubre algún técnico pero no hay horarios libres
     * (SIN_HORARIOS) en {@code fecha}, el día que el cliente consultaba (null = hoy; no puede ser pasada ni superar 90 días).
     */
    Cita solicitar(String correoCliente, Long especialidadId, String descripcionProblema,
                   String direccionServicio, Double latitud, Double longitud, String telefonoContacto, java.time.LocalDate fecha);
}
