package com.appointech.appointech_backend.domain.models;

import java.util.List;

public record CitaConCliente(Cita cita, ClienteResumen cliente, List<CitaFoto> fotos) {

    /** Cita sin fotos del electrodoméstico. */
    public CitaConCliente(Cita cita, ClienteResumen cliente) {
        this(cita, cliente, List.of());
    }
}
