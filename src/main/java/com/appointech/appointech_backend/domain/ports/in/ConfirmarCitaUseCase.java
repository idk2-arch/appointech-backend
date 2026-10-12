package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Cita;

import java.time.LocalDateTime;

public interface ConfirmarCitaUseCase {
    Cita confirmar(String correoCliente, Long solicitudId, Long tecnicoId, LocalDateTime inicio, LocalDateTime fin);
}
