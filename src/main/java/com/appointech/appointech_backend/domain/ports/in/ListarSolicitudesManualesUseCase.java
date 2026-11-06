package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.FiltroSolicitudesManuales;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.SolicitudManualDetalle;

import java.util.List;

public interface ListarSolicitudesManualesUseCase {
    /** Todas las solicitudes manuales pendientes, las más antiguas primero. */
    List<SolicitudManualDetalle> listar();

    /**
     * Solicitudes manuales filtradas (estado PENDIENTE si no se indica) y paginadas si se pide, más antiguas primero.
     *
     * @throws com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException si la página o el tamaño no son válidos
     */
    ListadoConTotal<SolicitudManualDetalle> listar(FiltroSolicitudesManuales filtro);
}
