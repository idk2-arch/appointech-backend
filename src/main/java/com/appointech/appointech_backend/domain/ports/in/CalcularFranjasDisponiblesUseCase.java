package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.FranjaDisponible;
import com.appointech.appointech_backend.domain.models.ResultadoFranjas;

import java.time.LocalDate;
import java.util.List;

public interface CalcularFranjasDisponiblesUseCase {
    List<FranjaDisponible> calcular(Long especialidadId, LocalDate fecha, Double latitud, Double longitud);

    /** Igual que {@link #calcular}, pero si no hay franjas explica el motivo. */
    ResultadoFranjas calcularConMotivo(Long especialidadId, LocalDate fecha, Double latitud, Double longitud);
}
