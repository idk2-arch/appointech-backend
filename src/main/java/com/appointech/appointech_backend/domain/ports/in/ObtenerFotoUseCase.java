package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.FotoAlmacenada;

public interface ObtenerFotoUseCase {

    /** Lanza FotoNoEncontradaException si la clave no tiene formato UUID o no existe. */
    FotoAlmacenada obtener(String clave);
}
