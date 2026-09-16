package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Usuario;

public interface ActualizarUsuarioUseCase {
    Usuario actualizar(Long id, String nombre, String apellido, String telefono);
}