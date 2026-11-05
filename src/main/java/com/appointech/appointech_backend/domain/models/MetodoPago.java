package com.appointech.appointech_backend.domain.models;

public enum MetodoPago {
    EFECTIVO("Efectivo"),
    EN_LINEA("En línea");

    private final String etiqueta;

    MetodoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
