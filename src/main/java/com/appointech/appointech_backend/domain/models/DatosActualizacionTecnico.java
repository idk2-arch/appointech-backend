package com.appointech.appointech_backend.domain.models;

import java.util.List;

/**
 * Cambios que el administrador puede aplicar a un técnico. Un valor null significa "no modificar".
 * especialidadIds reemplaza el conjunto completo (una lista vacía las quita todas).
 */
public record DatosActualizacionTecnico(
        String nombre,
        String apellido,
        String correo,
        String telefono,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String direccionBase,
        Integer radioCoberturaKm,
        Double latitudBase,
        Double longitudBase,
        List<Long> especialidadIds
) {
}
