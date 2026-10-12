package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Cita;

public interface CrearSolicitudServicioUseCase {
    Cita crear(String correoCliente, Long especialidadId, String descripcionProblema,
               String direccionServicio, Double latitud, Double longitud);
}
