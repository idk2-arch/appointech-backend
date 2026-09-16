package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;

public interface CrearAdministradorUseCase {
    Usuario crear(String nombre, String apellido, String correo, String contrasena, String telefono,
                  TipoDocumento tipoDocumento, String numeroDocumento);
}