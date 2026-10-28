package com.appointech.appointech_backend.domain.ports.in;

public interface EliminarFotoCitaUseCase {

    /** El cliente dueño borra una foto de su cita (solo PENDIENTE o CONFIRMADA) y su objeto del almacenamiento. */
    void eliminar(String correoCliente, Long citaId, Long fotoId);
}
