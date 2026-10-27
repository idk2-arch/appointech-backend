package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.CoberturaConfigurada;

public interface ObtenerCoberturaUseCase {
    /** Límite único de distancia y teléfono de WhatsApp del administrador. */
    CoberturaConfigurada obtener();
}
