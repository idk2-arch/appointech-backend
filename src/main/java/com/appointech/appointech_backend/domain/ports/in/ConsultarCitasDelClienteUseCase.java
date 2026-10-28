package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.DetalleCitaCompleto;

import java.util.List;

public interface ConsultarCitasDelClienteUseCase {
    List<DetalleCita> listar(String correoCliente);

    /** Detalle con historial; un cliente solo accede a sus propias citas. */
    DetalleCitaCompleto detallar(String correoCliente, Long citaId);
}
