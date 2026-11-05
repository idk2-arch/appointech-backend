package com.appointech.appointech_backend.infrastructure.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record SincronizarPagoRequest(
        @Schema(description = "Id de la transacción que devolvió la pasarela. Opcional: si no llega, se consulta por la "
                + "referencia guardada en el pago.", nullable = true)
        @Size(max = 100, message = "El id de la transacción no puede superar 100 caracteres")
        String idTransaccion
) {
}
