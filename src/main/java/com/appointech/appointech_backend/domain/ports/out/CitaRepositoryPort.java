package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.FiltroSolicitudesManuales;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CitaRepositoryPort {
    Cita guardar(Cita cita);
    Optional<Cita> buscarPorId(Long id);
    /** Solicitudes de agendamiento manual del estado indicado (el filtro trae siempre estado), las más antiguas primero; paginadas solo si el filtro lo pide. */
    ListadoConTotal<Cita> buscarSolicitudesManuales(FiltroSolicitudesManuales filtro);
    /** Todas las citas del cliente, las más recientes primero. */
    List<Cita> listarPorCliente(Long clienteId);
    List<Cita> buscarActivasDelTecnicoEntre(Long tecnicoId, LocalDateTime inicio, LocalDateTime fin);
}
