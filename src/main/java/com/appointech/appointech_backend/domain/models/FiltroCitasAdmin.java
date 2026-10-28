package com.appointech.appointech_backend.domain.models;

import java.time.LocalDate;

/** Filtros opcionales (null = sin filtrar) y paginación (página desde 0) de la consulta de citas del administrador. */
public record FiltroCitasAdmin(
        Long tecnicoId,
        EstadoCita estado,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        int pagina,
        int tamanio
) {
}
