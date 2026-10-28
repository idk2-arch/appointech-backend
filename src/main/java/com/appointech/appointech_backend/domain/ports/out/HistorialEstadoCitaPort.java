package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.HistorialEstadoCita;

import java.util.List;

public interface HistorialEstadoCitaPort {
    HistorialEstadoCita registrar(HistorialEstadoCita entrada);

    /** Del más antiguo al más reciente. */
    List<HistorialEstadoCita> listarPorCita(Long citaId);
}
