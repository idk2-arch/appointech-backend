package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.Cita;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CitaRepositoryPort {
    Cita guardar(Cita cita);
    Optional<Cita> buscarPorId(Long id);
    /** Solicitudes de agendamiento manual que siguen PENDIENTES (sin técnico), las más antiguas primero. */
    List<Cita> buscarSolicitudesManualesPendientes();
    /** Todas las citas del cliente, las más recientes primero. */
    List<Cita> listarPorCliente(Long clienteId);
    List<Cita> buscarActivasDelTecnicoEntre(Long tecnicoId, LocalDateTime inicio, LocalDateTime fin);
}
