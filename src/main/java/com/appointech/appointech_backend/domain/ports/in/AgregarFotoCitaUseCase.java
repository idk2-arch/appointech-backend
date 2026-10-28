package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.CitaFoto;

public interface AgregarFotoCitaUseCase {

    /** El cliente dueño adjunta una foto a su cita (máximo 3, solo PENDIENTE o CONFIRMADA). */
    CitaFoto agregar(String correoCliente, Long citaId, String imagenBase64);
}
