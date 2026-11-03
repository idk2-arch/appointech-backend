package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.PreDiagnostico;

public interface GenerarPreDiagnosticoUseCase {
    PreDiagnostico generar(String descripcionProblema, String especialidad);
}
