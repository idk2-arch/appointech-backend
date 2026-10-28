package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.Cita;

public interface NotificadorCitaPort {
    void notificarCitaConfirmada(Cita cita, String correoCliente);

    /** Avisa a un participante de la cita (cliente o técnico) de un cambio de estado, cancelación o reprogramación. */
    void notificarCambioDeCita(Cita cita, String correoDestinatario, String mensaje);
}
