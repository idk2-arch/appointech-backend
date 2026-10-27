package com.appointech.appointech_backend.domain.models;

/** Cambios que el administrador puede aplicar a un cliente. Un valor null significa "no modificar". */
public record DatosActualizacionCliente(
        String nombre,
        String apellido,
        String correo,
        String telefono,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String direccion,
        Double latitud,
        Double longitud
) {
}
