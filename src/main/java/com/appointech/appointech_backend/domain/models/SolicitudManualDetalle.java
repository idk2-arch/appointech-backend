package com.appointech.appointech_backend.domain.models;

import java.time.LocalDateTime;

/** Modelo de lectura de una solicitud de agendamiento manual con los datos del cliente y la especialidad. */
public record SolicitudManualDetalle(
        Long id,
        Long clienteId,
        String clienteNombre,
        String clienteApellido,
        String telefonoContacto,
        String direccionServicio,
        String descripcionProblema,
        Long especialidadId,
        String especialidadNombre,
        Double latitud,
        Double longitud,
        LocalDateTime creadaEn
) {
}
