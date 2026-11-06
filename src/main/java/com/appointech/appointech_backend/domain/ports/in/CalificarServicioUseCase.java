package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Calificacion;

public interface CalificarServicioUseCase {

    /**
     * El cliente dueño califica su cita finalizada.
     *
     * @throws com.appointech.appointech_backend.domain.exception.CalificacionInvalidaException si la puntuación o el comentario no son válidos
     * @throws com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException si la cita no existe o no es del cliente
     * @throws com.appointech.appointech_backend.domain.exception.CitaNoFinalizadaException si la cita no está finalizada
     * @throws com.appointech.appointech_backend.domain.exception.CitaYaCalificadaException si ya tiene calificación
     * @throws com.appointech.appointech_backend.domain.exception.PlazoCalificacionVencidoException si pasó el plazo
     */
    Calificacion calificar(String correoCliente, Long citaId, Integer puntuacion, String comentario);
}
