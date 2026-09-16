package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.Usuario;

public interface TokenGeneratorPort {
    String generarToken(Usuario usuario);
}