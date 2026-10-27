package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.CoberturaConfigurada;

public interface ActualizarCoberturaUseCase {
    /**
     * Cambia el límite único de distancia (1 a 100 km). El teléfono de WhatsApp null no se modifica;
     * vacío lo borra; con valor se valida.
     */
    CoberturaConfigurada actualizar(Integer radioMaximoKm, String telefonoWhatsappAdmin);
}
