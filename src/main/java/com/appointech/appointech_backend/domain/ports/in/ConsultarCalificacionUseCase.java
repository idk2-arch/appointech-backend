package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Calificacion;

public interface ConsultarCalificacionUseCase {

    /**
     * La calificación de una cita del cliente.
     *
     * @throws com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException si la cita no existe o no es del cliente
     * @throws com.appointech.appointech_backend.domain.exception.CalificacionNoEncontradaException si aún no tiene calificación
     */
    Calificacion consultar(String correoCliente, Long citaId);
}
