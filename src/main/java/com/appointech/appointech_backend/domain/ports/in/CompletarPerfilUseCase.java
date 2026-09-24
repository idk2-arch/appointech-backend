package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;

public interface CompletarPerfilUseCase {
    Usuario completar(String correoUsuario, String telefono, TipoDocumento tipoDocumento,
                      String numeroDocumento, String direccion);
}