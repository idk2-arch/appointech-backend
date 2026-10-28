package com.appointech.appointech_backend.domain.models;

public record ClienteResumen(String nombre, String apellido, String telefono, String fotoClave) {

    /** Resumen sin foto. */
    public ClienteResumen(String nombre, String apellido, String telefono) {
        this(nombre, apellido, telefono, null);
    }
}
