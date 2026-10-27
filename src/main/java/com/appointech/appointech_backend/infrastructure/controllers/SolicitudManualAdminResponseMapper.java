package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.SolicitudManualDetalle;
import com.appointech.appointech_backend.infrastructure.dto.SolicitudManualAdminResponse;

final class SolicitudManualAdminResponseMapper {

    private SolicitudManualAdminResponseMapper() {
    }

    static SolicitudManualAdminResponse aRespuesta(SolicitudManualDetalle d) {
        return new SolicitudManualAdminResponse(
                d.id(), d.clienteId(), d.clienteNombre(), d.clienteApellido(), d.telefonoContacto(),
                d.direccionServicio(), d.descripcionProblema(), d.especialidadId(), d.especialidadNombre(),
                d.latitud(), d.longitud(), d.creadaEn());
    }
}
