package com.appointech.appointech_backend.domain.ports.in;

public interface ActualizarFotoUsuarioUseCase {

    /** El administrador guarda o reemplaza la foto de otro usuario y recibe la clave nueva. */
    String reemplazar(Long usuarioId, String imagenBase64);
}
