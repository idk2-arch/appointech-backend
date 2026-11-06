package com.appointech.appointech_backend.application.services;

import com.appointech.appointech_backend.domain.exception.FranjaInvalidaException;
import com.appointech.appointech_backend.domain.exception.FranjaNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.ports.out.BloqueIndisponibilidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reglas compartidas por la consulta de técnicos disponibles y por la asignación manual (RF15), para que ambas
 * coincidan: qué solicitud se puede asignar y cuándo un técnico está ocupado.
 */
@Component
public class ReglasAsignacionManual {

    private final CitaRepositoryPort citaRepositoryPort;
    private final BloqueIndisponibilidadRepositoryPort bloqueIndisponibilidadRepositoryPort;

    public ReglasAsignacionManual(CitaRepositoryPort citaRepositoryPort,
                                  BloqueIndisponibilidadRepositoryPort bloqueIndisponibilidadRepositoryPort) {
        this.citaRepositoryPort = citaRepositoryPort;
        this.bloqueIndisponibilidadRepositoryPort = bloqueIndisponibilidadRepositoryPort;
    }

    /** El fin debe ser posterior al inicio. */
    public void exigirRangoValido(LocalDateTime inicio, LocalDateTime fin) {
        if (!fin.isAfter(inicio)) {
            throw new FranjaInvalidaException("La hora de fin debe ser posterior a la de inicio");
        }
    }

    /** La solicitud manual que sigue pendiente y sin técnico; 404 si no existe o no es manual, 409 si ya se asignó. */
    public Cita solicitudPendiente(Long solicitudId) {
        Cita solicitud = citaRepositoryPort.buscarPorId(solicitudId)
                .filter(Cita::isAgendamientoManual)
                .orElseThrow(() -> new SolicitudNoEncontradaException(solicitudId));
        if (solicitud.getEstado() != EstadoCita.PENDIENTE || solicitud.getTecnicoId() != null) {
            throw new FranjaNoDisponibleException("La solicitud ya no está pendiente");
        }
        return solicitud;
    }

    /** Hay otra cita activa o un bloqueo del técnico que se cruza con el horario (la propia solicitud no cuenta). */
    public boolean tecnicoOcupado(Long tecnicoId, Long solicitudId, LocalDateTime inicio, LocalDateTime fin) {
        List<Cita> citas = citaRepositoryPort.buscarActivasDelTecnicoEntre(tecnicoId, inicio, fin);
        List<BloqueIndisponibilidad> bloques = bloqueIndisponibilidadRepositoryPort.buscarDelTecnicoEntre(tecnicoId, inicio, fin);
        return citas.stream().anyMatch(cita -> !cita.getId().equals(solicitudId)
                && cita.getFechaHoraInicio().isBefore(fin) && cita.getFechaHoraFin().isAfter(inicio))
                || bloques.stream().anyMatch(bloque -> bloque.getInicio().isBefore(fin) && bloque.getFin().isAfter(inicio));
    }
}
