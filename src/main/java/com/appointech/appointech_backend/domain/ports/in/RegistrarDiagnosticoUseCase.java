package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Cita;

public interface RegistrarDiagnosticoUseCase {
    /** Crea o actualiza el diagnóstico de la cita mientras esté EN_PROCESO. */
    Cita registrar(String correoTecnico, Long citaId, String diagnostico, String recomendaciones);
}
