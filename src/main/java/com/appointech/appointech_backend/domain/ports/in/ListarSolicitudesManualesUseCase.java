package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.SolicitudManualDetalle;

import java.util.List;

public interface ListarSolicitudesManualesUseCase {
    List<SolicitudManualDetalle> listar();
}
