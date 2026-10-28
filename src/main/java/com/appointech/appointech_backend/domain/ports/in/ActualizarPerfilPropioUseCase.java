package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.DatosPerfilEditable;
import com.appointech.appointech_backend.domain.models.PerfilPropio;

public interface ActualizarPerfilPropioUseCase {
    PerfilPropio actualizar(String correoUsuario, DatosPerfilEditable datos);
}
