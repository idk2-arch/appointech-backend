package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.RutaDelDia;

import java.time.LocalDate;

public interface CalcularRutaDelDiaUseCase {
    RutaDelDia calcular(String correoTecnico, LocalDate fecha);
}
