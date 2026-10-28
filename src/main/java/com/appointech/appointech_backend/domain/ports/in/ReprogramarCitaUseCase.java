package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Cita;

import java.time.LocalDateTime;

public interface ReprogramarCitaUseCase {
    /** El cliente dueño mueve su cita a otra franja disponible del mismo técnico. */
    Cita reprogramar(String correoCliente, Long citaId, LocalDateTime nuevoInicio);
}
