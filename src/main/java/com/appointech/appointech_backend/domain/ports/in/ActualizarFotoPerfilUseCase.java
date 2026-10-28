package com.appointech.appointech_backend.domain.ports.in;

public interface ActualizarFotoPerfilUseCase {

    /** Guarda o reemplaza la foto del usuario autenticado y devuelve la clave nueva. */
    String reemplazar(String correo, String imagenBase64);

    /** Borra la foto del usuario autenticado; no hace nada si no tenía. */
    void eliminar(String correo);
}
