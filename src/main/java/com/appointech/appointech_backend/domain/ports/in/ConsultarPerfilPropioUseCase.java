package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.PerfilPropio;

public interface ConsultarPerfilPropioUseCase {
    PerfilPropio consultar(String correoUsuario);
}
