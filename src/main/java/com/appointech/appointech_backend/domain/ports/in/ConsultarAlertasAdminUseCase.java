package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.SolicitudManualDetalle;

import java.util.List;

public interface ConsultarAlertasAdminUseCase {
    /**
     * Alertas vigentes para el administrador, derivadas de las solicitudes PENDIENTES marcadas con
     * SIN_TECNICOS_ESPECIALIDAD (RF28), las más antiguas primero. Dejan de aparecer cuando la solicitud se atiende.
     */
    List<SolicitudManualDetalle> sinTecnicosParaLaEspecialidad();
}
