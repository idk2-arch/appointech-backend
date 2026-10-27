package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.DatosActualizacionCliente;

public interface ActualizarClienteUseCase {
    Cliente actualizar(Long clienteId, DatosActualizacionCliente datos);
}
