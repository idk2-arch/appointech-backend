package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.FotoAlmacenada;

public interface ObtenerFotoCitaUseCase {

    /** Solo el cliente dueño, el técnico asignado y el administrador; si no, AccesoCitaDenegadoException. */
    FotoAlmacenada obtener(String correo, Long citaId, Long fotoId);
}
