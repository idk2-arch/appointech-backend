package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.PreDiagnostico;

public interface PreDiagnosticoPort {
    /**
     * Genera una orientación a partir de la descripción del problema y la especialidad (null si no se indicó).
     * Nunca lanza excepciones: si el servicio no responde devuelve {@link PreDiagnostico#noDisponible()}.
     */
    PreDiagnostico generar(String descripcionProblema, String especialidad);
}
