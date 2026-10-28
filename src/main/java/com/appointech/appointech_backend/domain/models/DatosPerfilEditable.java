package com.appointech.appointech_backend.domain.models;

/** Únicos datos que un usuario puede cambiar de su propio perfil; direccion null = sin cambio. */
public record DatosPerfilEditable(String nombre, String apellido, String telefono, String direccion) {
}
