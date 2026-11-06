package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.TecnicoDisponible;

import java.time.LocalDateTime;
import java.util.List;

public interface ConsultarTecnicosDisponiblesUseCase {

    /**
     * Técnicos activos con la especialidad de la solicitud manual y sin citas ni bloqueos que se crucen con el
     * horario, del más cercano al cliente al más lejano; los que no tienen ubicación van al final.
     *
     * @throws com.appointech.appointech_backend.domain.exception.FranjaInvalidaException si el fin no es posterior al inicio
     * @throws com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException si la solicitud no existe o no es manual
     * @throws com.appointech.appointech_backend.domain.exception.FranjaNoDisponibleException si la solicitud ya no está pendiente
     */
    List<TecnicoDisponible> consultar(Long solicitudId, LocalDateTime inicio, LocalDateTime fin);
}
