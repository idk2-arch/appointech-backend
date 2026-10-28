package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.FotoAlmacenada;
import com.appointech.appointech_backend.domain.ports.out.AlmacenamientoFotosPort;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Almacenamiento falso en memoria: demuestra que el caso de uso funciona con cualquier implementación del puerto. */
public class AlmacenamientoFotosEnMemoria implements AlmacenamientoFotosPort {

    private final Map<String, FotoAlmacenada> objetos = new HashMap<>();

    @Override
    public void guardar(String clave, byte[] contenido, String tipoContenido) {
        objetos.put(clave, new FotoAlmacenada(contenido, tipoContenido));
    }

    @Override
    public Optional<FotoAlmacenada> obtener(String clave) {
        return Optional.ofNullable(objetos.get(clave));
    }

    @Override
    public void eliminar(String clave) {
        objetos.remove(clave);
    }

    public Set<String> claves() {
        return objetos.keySet();
    }
}
