package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;

import java.time.LocalDate;
import java.util.List;

public interface ListarBloquesUseCase {
    /** Sin {@code desde} se usa hoy; sin {@code hasta} no hay tope. Ambas fechas son inclusivas. */
    List<BloqueIndisponibilidad> listar(String correoTecnico, LocalDate desde, LocalDate hasta);
}
