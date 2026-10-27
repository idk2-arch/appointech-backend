package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Cita;

import java.time.LocalDateTime;

public interface AsignarSolicitudManualUseCase {
    Cita asignar(String correoAdministrador, Long solicitudId, Long tecnicoId, LocalDateTime inicio, LocalDateTime fin);
}
