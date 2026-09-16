package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Usuario;

public interface CambiarEstadoUsuarioUseCase {
    Usuario cambiarEstado(Long id, boolean activo);
}