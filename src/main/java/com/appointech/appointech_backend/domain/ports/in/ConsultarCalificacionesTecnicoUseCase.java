package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.CalificacionesDelTecnico;

public interface ConsultarCalificacionesTecnicoUseCase {

    /**
     * Calificaciones recibidas por el técnico autenticado, con su promedio. Página y tamaño son opcionales.
     *
     * @throws com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException si la página o el tamaño no son válidos
     */
    CalificacionesDelTecnico consultar(String correoTecnico, Integer pagina, Integer tamanio);
}
