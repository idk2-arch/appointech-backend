package com.appointech.appointech_backend.domain.models;

import java.time.LocalDate;

/**
 * Filtros opcionales (null = sin filtrar) y paginación (página desde 0) del historial de pagos.
 * {@code desde} y {@code hasta} son fechas inclusivas sobre la fecha de pago; usar cualquiera de las dos excluye
 * los pagos que aún no tienen fecha de pago. Al llegar al puerto de salida la página y el tamaño ya vienen completos.
 */
public record FiltroHistorialPagos(
        EstadoPago estado,
        MetodoPago metodo,
        Long clienteId,
        Long tecnicoId,
        LocalDate desde,
        LocalDate hasta,
        Integer pagina,
        Integer tamanio
) {
}
