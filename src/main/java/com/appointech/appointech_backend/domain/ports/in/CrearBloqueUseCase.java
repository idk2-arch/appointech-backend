package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;

import java.time.LocalDateTime;

public interface CrearBloqueUseCase {
    BloqueIndisponibilidad crear(String correoTecnico, LocalDateTime inicio, LocalDateTime fin, String motivo);
}
