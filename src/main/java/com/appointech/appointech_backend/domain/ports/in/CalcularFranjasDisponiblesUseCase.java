package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.FranjaDisponible;

import java.time.LocalDate;
import java.util.List;

public interface CalcularFranjasDisponiblesUseCase {
    List<FranjaDisponible> calcular(Long especialidadId, LocalDate fecha, Double latitud, Double longitud);
}
