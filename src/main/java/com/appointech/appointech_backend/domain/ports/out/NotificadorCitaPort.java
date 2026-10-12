package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.Cita;

public interface NotificadorCitaPort {
    void notificarCitaConfirmada(Cita cita, String correoCliente);
}
