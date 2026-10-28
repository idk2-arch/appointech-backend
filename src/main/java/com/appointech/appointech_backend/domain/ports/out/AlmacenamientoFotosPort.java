package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.FotoAlmacenada;

import java.util.Optional;

public interface AlmacenamientoFotosPort {

    /** Guarda (o sobrescribe) el objeto; lanza AlmacenamientoNoDisponibleException si el almacenamiento no responde. */
    void guardar(String clave, byte[] contenido, String tipoContenido);

    /** Devuelve la foto, o vacío si la clave no existe. */
    Optional<FotoAlmacenada> obtener(String clave);

    /** Elimina el objeto; no falla si ya no existe. */
    void eliminar(String clave);
}
