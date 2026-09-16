package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.TipoDocumento;

public interface RegistrarClienteUseCase {
    Cliente registrar(String nombre, String apellido, String correo, String contrasena, String telefono,
                      TipoDocumento tipoDocumento, String numeroDocumento, String direccion);
}