package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.RedaccionTecnica;

public interface RedactorTecnicoPort {
    /**
     * Reescribe la descripción del cliente en lenguaje técnico (la especialidad es null si no se indicó).
     * Nunca lanza excepciones: si el servicio no responde devuelve {@link RedaccionTecnica#noDisponible()}.
     */
    RedaccionTecnica redactar(String descripcionProblema, String especialidad);
}
