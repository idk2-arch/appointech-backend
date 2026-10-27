package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Cita;

public interface SolicitarAgendamientoManualUseCase {
    /** Solo permitido cuando la ubicación está FUERA_DE_COBERTURA. */
    Cita solicitar(String correoCliente, Long especialidadId, String descripcionProblema,
                   String direccionServicio, Double latitud, Double longitud, String telefonoContacto);
}
