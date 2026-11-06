package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.Calificacion;
import com.appointech.appointech_backend.domain.models.CalificacionRecibida;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;

import java.util.Optional;

public interface CalificacionRepositoryPort {

    /** Guarda una calificación nueva; si la cita ya tenía una, falla con CitaYaCalificadaException. */
    Calificacion guardar(Calificacion calificacion);

    Optional<Calificacion> buscarPorCitaId(Long citaId);

    /** Calificaciones de las citas del técnico, las más recientes primero; paginadas solo si se pide. El total es el de todas. */
    ListadoConTotal<CalificacionRecibida> listarRecibidasPorTecnico(Long tecnicoId, PaginacionOpcional paginacion);

    /** Promedio de las calificaciones del técnico (sin redondear); vacío si no tiene ninguna. */
    Optional<Double> promedioDelTecnico(Long tecnicoId);
}
