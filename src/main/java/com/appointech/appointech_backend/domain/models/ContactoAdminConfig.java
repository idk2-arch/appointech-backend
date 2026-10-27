package com.appointech.appointech_backend.domain.models;

/** Teléfono de respaldo del administrador (variable de entorno) para cuando no hay uno guardado en base de datos. */
public record ContactoAdminConfig(String telefonoRespaldo) {

    /** El de la base de datos si tiene valor; si no, el de respaldo; null si no hay ninguno. */
    public String resolver(String telefonoEnBaseDeDatos) {
        if (telefonoEnBaseDeDatos != null && !telefonoEnBaseDeDatos.isBlank()) {
            return telefonoEnBaseDeDatos;
        }
        if (telefonoRespaldo != null && !telefonoRespaldo.isBlank()) {
            return telefonoRespaldo.trim();
        }
        return null;
    }
}
